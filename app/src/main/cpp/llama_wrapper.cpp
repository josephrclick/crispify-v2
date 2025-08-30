#include "llama_wrapper.h"
#include <android/log.h>
#include <thread>
#include <chrono>
#include <sstream>
#include <vector>
#include <string>
#include <algorithm>
#include <cmath>
#include <cctype>
#include <fstream>
#include "llama.h"
#include "common.h"
#include "sampling.h"
#include "chat.h"

#define LOG_TAG "LlamaWrapper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Error codes for inference
enum class InferenceError {
    NONE = 0,
    TOKEN_LIMIT_EXCEEDED = 1,    // Input exceeds 1000 tokens
    INFERENCE_FAILED = 2,         // llama.cpp decode error
    OUT_OF_MEMORY = 3,           // Memory allocation failed
    MODEL_NOT_LOADED = 4,        // Model not initialized
    CONTEXT_OVERFLOW = 5,        // Total prompt exceeds context
    CANCELLED = 6                // User cancelled operation
};

// Create sampling parameters for text simplification
common_params_sampling createSamplingParams() {
    common_params_sampling params;
    // Optimized for Gemma-3 270M QAT model
    params.temp = 0.8f;              // Slightly creative but consistent
    params.top_p = 0.92f;            // Nucleus sampling - balanced diversity
    params.top_k = 50;               // Top-k filtering - reasonable variety
    params.min_p = 0.05f;            // Min-p threshold to filter low probability tokens
    params.penalty_repeat = 1.1f;    // Moderate repetition penalty
    params.penalty_last_n = 256;     // Longer lookback for repetition
    params.penalty_freq = 0.02f;     // Slight frequency penalty
    params.penalty_present = 0.02f;  // Slight presence penalty
    return params;
}

// Implementation details (pImpl pattern for ABI stability)
struct LlamaWrapper::Impl {
    bool model_loaded = false;
    size_t memory_usage = 0;
    
    // llama.cpp context and model
    llama_context* ctx = nullptr;
    llama_model* model = nullptr;
    
    // Sampling context
    common_sampler* sampling_ctx = nullptr;
    
    // Chat template support
    common_chat_templates_ptr chat_templates{nullptr};
    
    // Helper function to check available memory on Android
    size_t getAvailableMemory() {
        std::ifstream meminfo("/proc/meminfo");
        std::string line;
        size_t available_kb = 0;
        
        while (std::getline(meminfo, line)) {
            if (line.find("MemAvailable:") == 0) {
                std::istringstream iss(line);
                std::string label;
                iss >> label >> available_kb;
                break;
            }
        }
        
        return available_kb * 1024; // Convert to bytes
    }
    
    // Helper function to determine text complexity for adaptive prompting
    int countWords(const std::string& text) {
        int count = 0;
        bool in_word = false;
        for (char c : text) {
            // Cast to unsigned char to avoid undefined behavior with non-ASCII UTF-8 characters
            if (std::isspace(static_cast<unsigned char>(c))) {
                if (in_word) {
                    count++;
                    in_word = false;
                }
            } else {
                in_word = true;
            }
        }
        if (in_word) count++;
        return count;
    }
    
    // Count tokens in text (pre-flight check)
    int countTokens(const std::string& text) {
        if (!model || !ctx) return -1;
        
        const llama_vocab* vocab = llama_model_get_vocab(model);
        std::vector<llama_token> tokens(text.length() + 1);
        int n_tokens = llama_tokenize(
            vocab,
            text.c_str(),
            text.length(),
            tokens.data(),
            tokens.size(),
            true,  // add_bos
            false  // special
        );
        
        if (n_tokens < 0) {
            // Resize and retry if buffer was too small
            tokens.resize(-n_tokens);
            n_tokens = llama_tokenize(
                vocab,
                text.c_str(),
                text.length(),
                tokens.data(),
                tokens.size(),
                true,  // add_bos
                false  // special
            );
        }
        
        return n_tokens;
    }
};

LlamaWrapper::LlamaWrapper() : pImpl(std::make_unique<Impl>()) {
    LOGD("LlamaWrapper created");
}

LlamaWrapper::~LlamaWrapper() {
    if (pImpl->model_loaded) {
        releaseModel();
    }
    LOGD("LlamaWrapper destroyed");
}

bool LlamaWrapper::loadModel(const std::string& model_path, ProgressCallback progress_cb) {
    LOGD("Loading model from: %s", model_path.c_str());
    
    // Initialize llama backend
    llama_backend_init();
    
    // Initialize model parameters
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU-only for now
    
    // Progress callback at 10%
    if (progress_cb) progress_cb(0.1f);
    
    // Load the model
    pImpl->model = llama_model_load_from_file(model_path.c_str(), model_params);
    if (!pImpl->model) {
        LOGE("Failed to load model from %s", model_path.c_str());
        return false;
    }
    
    // Progress callback at 50%
    if (progress_cb) progress_cb(0.5f);
    
    // Initialize context parameters (optimized for mobile)
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;        // Context window
    ctx_params.n_batch = 128;       // Reduced from 512 for mobile
    ctx_params.n_ubatch = 128;      // Physical batch size
    ctx_params.n_threads = 4;       // CPU threads
    ctx_params.n_threads_batch = 4; // Batch processing threads
    
    // Create context
    pImpl->ctx = llama_init_from_model(pImpl->model, ctx_params);
    if (!pImpl->ctx) {
        LOGE("Failed to create context");
        llama_model_free(pImpl->model);
        pImpl->model = nullptr;
        return false;
    }
    
    // Progress callback at 90%
    if (progress_cb) progress_cb(0.9f);
    
    // Initialize sampling context
    auto sparams = createSamplingParams();
    pImpl->sampling_ctx = common_sampler_init(pImpl->model, sparams);
    if (!pImpl->sampling_ctx) {
        LOGE("Failed to create sampling context");
        llama_free(pImpl->ctx);
        pImpl->ctx = nullptr;
        llama_model_free(pImpl->model);
        pImpl->model = nullptr;
        return false;
    }
    
    pImpl->model_loaded = true;
    
    // Get actual memory usage from llama.cpp
    uint64_t model_size = llama_model_size(pImpl->model);
    size_t context_size = llama_state_get_size(pImpl->ctx);
    pImpl->memory_usage = model_size + context_size;
    
    // Initialize chat templates from model (if available)
    pImpl->chat_templates = common_chat_templates_init(pImpl->model, /*override*/ "");
    if (pImpl->chat_templates) {
        const char* src = common_chat_templates_source(pImpl->chat_templates.get(), nullptr);
        LOGD("Model chat template detected (source: %s)", src ? src : "unknown");
    } else {
        LOGD("Model chat template: none, using fallback formatting");
    }
    
    // Progress callback at 100%
    if (progress_cb) progress_cb(1.0f);
    
    LOGD("Model loaded successfully, memory: %zu bytes", pImpl->memory_usage);
    return true;
}

void LlamaWrapper::processText(const std::string& input_text, 
                               TokenCallback token_cb,
                               const std::atomic<bool>& cancel_flag) {
    if (!pImpl->model_loaded) {
        LOGE("Cannot process text - model not loaded");
        if (token_cb) {
            token_cb("", true);
        }
        return;
    }
    
    LOGD("Processing text of length: %zu", input_text.length());
    
    // Step 0: Check available memory before processing
    size_t available_memory = pImpl->getAvailableMemory();
    size_t required_memory = 100 * 1024 * 1024; // Require at least 100MB free
    
    if (available_memory < required_memory) {
        LOGE("Insufficient memory: %zu MB available, %zu MB required", 
             available_memory / (1024 * 1024), required_memory / (1024 * 1024));
        if (token_cb) {
            token_cb("", true);
        }
        return;
    }
    
    // Step 1: Build adaptive prompt based on input characteristics
    std::string sys_msg;
    std::string user_msg;
    
    // Count words for adaptive prompting
    int word_count = pImpl->countWords(input_text);
    LOGD("Input word count: %d", word_count);
    
    // Adaptive prompt engineering based on text length
    if (word_count <= 25) {
        // Very short text - focus on concise rewriting
        sys_msg = "You are a text simplifier. Rewrite text in simple, clear language. "
                  "Keep all facts and numbers. Use easy words. Output 1-2 sentences only.";
        
        user_msg = "Simplify this: " + input_text;
        
    } else if (word_count <= 75) {
        // Medium text - balanced simplification
        sys_msg = "You are an expert editor who simplifies complex text. "
                  "Follow instructions precisely. Your output must be clear, factual, and easy to read. "
                  "Write only the simplified version as 2-3 short sentences. "
                  "Keep all key facts, names, and numbers. Use simple words.";
        
        user_msg = "Rewrite the following text in clear, plain language suitable for a 7th-grade reading level:\n\n" 
                   + input_text;
                   
    } else {
        // Longer text - focus on key information extraction
        sys_msg = "You are an expert at extracting and simplifying key information. "
                  "Summarize the most important facts in 3-4 simple sentences. "
                  "Use plain language that anyone can understand. "
                  "Include all important names, numbers, and facts.";
        
        user_msg = "Extract and simplify the key information from this text:\n\n" + input_text;
    }
    
    // Step 2: Format messages using chat template if available
    std::string full_prompt;
    
    if (pImpl->chat_templates) {
        // Use model's built-in chat template with potential few-shot
        common_chat_templates_inputs base_inputs;
        base_inputs.use_jinja = true;
        base_inputs.messages.push_back({"system", sys_msg});
        base_inputs.messages.push_back({"user", user_msg});
        
        // Check if we have room for a few-shot example
        auto chat_params_base = common_chat_templates_apply(pImpl->chat_templates.get(), base_inputs);
        auto base_tokens = common_tokenize(pImpl->ctx, chat_params_base.prompt, false, true);
        const int base_n_tokens = (int) base_tokens.size();
        
        // Only include few-shot if we have plenty of room
        const bool include_demo = (base_n_tokens < 400 && word_count > 15);
        
        if (include_demo) {
            common_chat_templates_inputs inputs;
            inputs.use_jinja = true;
            inputs.messages.push_back({"system", sys_msg});
            
            // Add a relevant example
            const std::string demo_user = "Simplify this: New Mexico health officials said they have confirmed "
                                         "the first human case of the plague in the state in 2025, occurring in "
                                         "a 43-year-old male from Valencia County who recently went camping.";
            const std::string demo_assistant = "New Mexico confirmed its first plague case of 2025. "
                                              "The patient is a 43-year-old man from Valencia County who went camping recently.";
            
            inputs.messages.push_back({"user", demo_user});
            inputs.messages.push_back({"assistant", demo_assistant});
            inputs.messages.push_back({"user", user_msg});
            
            auto chat_params = common_chat_templates_apply(pImpl->chat_templates.get(), inputs);
            full_prompt = chat_params.prompt;
            LOGD("Using chat template with few-shot example");
        } else {
            full_prompt = chat_params_base.prompt;
            LOGD("Using chat template without few-shot (base tokens=%d)", base_n_tokens);
        }
    } else {
        // Fallback for models without chat templates
        full_prompt = sys_msg + "\n\nUser: " + user_msg + "\n\nAssistant: ";
        LOGD("Using fallback prompt formatting");
    }
    
    // Step 3: Tokenize prompt with special token handling
    std::vector<llama_token> prompt_tokens = common_tokenize(
        pImpl->ctx, 
        full_prompt, 
        false,  // add_special - let common_tokenize handle it
        true    // parse_special - parse special tokens
    );
    
    const int n_prompt_tokens = (int) prompt_tokens.size();
    
    // Validate token counts
    if (n_prompt_tokens > 1200) {
        LOGE("Total prompt exceeds 1200 tokens: %d", n_prompt_tokens);
        if (token_cb) token_cb("", true);
        return;
    }
    
    const int n_ctx = llama_n_ctx(pImpl->ctx);
    if (n_prompt_tokens >= n_ctx - 100) {
        LOGE("Prompt too large for context: %d tokens, context: %d", n_prompt_tokens, n_ctx);
        if (token_cb) token_cb("", true);
        return;
    }
    
    LOGD("Prompt tokens: %d, Context size: %d", n_prompt_tokens, n_ctx);
    
    // Step 4: Initialize batch with dynamic sizing (PR #13 critical fix)
    const int n_batch_ctx = (int) llama_n_batch(pImpl->ctx);
    llama_batch batch = llama_batch_init(n_batch_ctx, 0, 1);
    
    // Process prompt in chunks if needed
    for (int i = 0; i < n_prompt_tokens; ) {
        const int n_batch_tokens = std::min(n_batch_ctx, n_prompt_tokens - i);
        
        for (int j = 0; j < n_batch_tokens; j++) {
            common_batch_add(batch, prompt_tokens[i + j], i + j, {0}, false);
        }
        
        // Mark last token for logits only on final batch
        if (i + n_batch_tokens >= n_prompt_tokens) {
            batch.logits[batch.n_tokens - 1] = true;
        }
        
        if (llama_decode(pImpl->ctx, batch) != 0) {
            LOGE("Failed to process prompt batch starting at token %d", i);
            llama_batch_free(batch);
            if (token_cb) token_cb("", true);
            return;
        }
        
        i += n_batch_tokens;
        common_batch_clear(batch);
    }
    
    // Step 5: Generate response with streaming
    const auto gen_start = std::chrono::steady_clock::now();
    int n_cur = n_prompt_tokens;
    int n_decode = 0;
    
    // Adaptive max tokens based on input length
    int n_max_tokens;
    if (word_count <= 25) {
        n_max_tokens = 150;  // Short input -> short output
    } else if (word_count <= 75) {
        n_max_tokens = 300;  // Medium input -> medium output
    } else {
        n_max_tokens = 500;  // Long input -> longer summary
    }
    
    // Reset sampling context for this generation
    common_sampler_reset(pImpl->sampling_ctx);
    
    while (n_decode < n_max_tokens && !cancel_flag) {
        // Sample next token
        llama_token new_token_id = common_sampler_sample(
            pImpl->sampling_ctx,
            pImpl->ctx,
            -1,     // Use default
            false   // Don't apply grammar
        );
        
        // Accept the sampled token
        common_sampler_accept(pImpl->sampling_ctx, new_token_id, true);
        
        // Check for end of generation
        const llama_vocab* vocab = llama_model_get_vocab(pImpl->model);
        if (new_token_id == llama_vocab_eos(vocab)) {
            LOGD("EOS token reached (id=%d)", (int) new_token_id);
            break;
        }
        
        // Convert token to text
        char token_str[256];
        int token_len = llama_token_to_piece(
            vocab,
            new_token_id,
            token_str,
            sizeof(token_str),
            0,
            true  // special tokens
        );
        
        if (token_len > 0) {
            std::string token_text(token_str, token_len);
            
            // Stream token immediately to UI
            if (token_cb) {
                token_cb(token_text, false);
            }
        }
        
        // Prepare next batch
        common_batch_clear(batch);
        common_batch_add(batch, new_token_id, n_cur, {0}, true);
        n_cur++;
        
        // Decode next token
        if (llama_decode(pImpl->ctx, batch) != 0) {
            LOGE("Failed to decode token %d", n_decode);
            break;
        }
        
        n_decode++;
        
        // Log progress periodically
        if (n_decode % 50 == 0) {
            LOGD("Generated %d tokens so far", n_decode);
        }
    }
    
    // Clean up
    llama_batch_free(batch);
    
    // Calculate and log performance metrics
    const auto gen_end = std::chrono::steady_clock::now();
    const auto gen_ms = std::chrono::duration_cast<std::chrono::milliseconds>(gen_end - gen_start).count();
    double tps = gen_ms > 0 ? (n_decode * 1000.0) / (double) gen_ms : 0.0;
    
    if (cancel_flag) {
        LOGD("Text processing cancelled after %d tokens", n_decode);
    }
    
    LOGD("Text processing complete - generated %d tokens in %lld ms (%.2f tok/s)", 
         n_decode, (long long) gen_ms, tps);
    
    // Signal completion to callback
    if (token_cb) {
        token_cb("", true);
    }
}

void LlamaWrapper::releaseModel() {
    LOGD("Releasing model resources");
    
    // Clean up sampling context
    if (pImpl->sampling_ctx) {
        common_sampler_free(pImpl->sampling_ctx);
        pImpl->sampling_ctx = nullptr;
    }
    
    // Clean up llama.cpp resources
    if (pImpl->ctx) {
        llama_free(pImpl->ctx);
        pImpl->ctx = nullptr;
    }
    
    if (pImpl->model) {
        llama_model_free(pImpl->model);
        pImpl->model = nullptr;
    }
    
    pImpl->model_loaded = false;
    pImpl->memory_usage = 0;
    
    // Clean up backend
    llama_backend_free();
}

bool LlamaWrapper::isModelLoaded() const {
    return pImpl->model_loaded;
}

size_t LlamaWrapper::getMemoryUsage() const {
    return pImpl->memory_usage;
}