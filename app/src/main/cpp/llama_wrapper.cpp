#include "llama_wrapper.h"
#include <android/log.h>
#include <thread>
#include <chrono>
#include <sstream>
#include <vector>
#include <string>
#include <algorithm>
#include <fstream>
#include "llama.h"
#include "common.h"
#include "sampling.h"

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
    params.temp = 0.7f;              // Temperature
    params.top_p = 0.9f;             // Nucleus sampling
    params.top_k = 40;               // Top-k filtering
    params.penalty_repeat = 1.05f;   // Slight repetition penalty
    params.penalty_last_n = 64;      // Lookback for repetition
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
    
    // Helper function to build the complete prompt
    std::string buildPrompt(const std::string& user_input) {
        // System preface (internal context)
        std::string system = "You are an expert editor who simplifies complex text. "
                             "You follow instructions precisely. Your output must be "
                             "clear, factual, and easy to read. You will end your "
                             "response with a single line that says: ### End";
        
        // User-visible prompt template
        std::string prompt = "### Simplified Text\n\n"
                            "Rewrite the following text in clear, plain language "
                            "suitable for a 7th-grade reading level. Preserve all "
                            "key facts, names, and numbers. Use shorter sentences "
                            "and simple words. Do not add any new information or "
                            "opinions.\n\n"
                            "Original Text:\n" + user_input;
        
        return system + "\n\n" + prompt;
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
            token_cb("", true); // Signal error with InferenceError::OUT_OF_MEMORY
        }
        return;
    }
    
    // Step 1: Validate input token count (must be under 1000)
    int input_tokens = pImpl->countTokens(input_text);
    if (input_tokens > 1000) {
        LOGE("Input text exceeds 1000 tokens: %d", input_tokens);
        if (token_cb) {
            token_cb("", true);  // Signal error
        }
        return;
    }
    
    // Step 2: Build complete prompt with template
    std::string full_prompt = pImpl->buildPrompt(input_text);
    
    // Step 3: Check total prompt tokens (must be under 1200)
    int total_tokens = pImpl->countTokens(full_prompt);
    if (total_tokens > 1200) {
        LOGE("Total prompt exceeds 1200 tokens: %d", total_tokens);
        if (token_cb) {
            token_cb("", true);
        }
        return;
    }
    
    LOGD("Input tokens: %d, Total prompt tokens: %d", input_tokens, total_tokens);
    
    // Step 4: Tokenize the prompt
    const llama_vocab* vocab = llama_model_get_vocab(pImpl->model);
    std::vector<llama_token> prompt_tokens(total_tokens + 1);
    int n_prompt_tokens = llama_tokenize(
        vocab,
        full_prompt.c_str(),
        full_prompt.length(),
        prompt_tokens.data(),
        prompt_tokens.size(),
        true,   // add_bos
        false   // special
    );
    
    if (n_prompt_tokens < 0) {
        LOGE("Failed to tokenize prompt");
        if (token_cb) {
            token_cb("", true);
        }
        return;
    }
    
    prompt_tokens.resize(n_prompt_tokens);
    
    // Step 5: Initialize batch for processing
    llama_batch batch = llama_batch_init(128, 0, 1);
    
    // Add prompt tokens to batch
    for (int i = 0; i < n_prompt_tokens; i++) {
        common_batch_add(batch, prompt_tokens[i], i, {0}, false);
    }
    
    // Mark last token for generation
    batch.logits[batch.n_tokens - 1] = true;
    
    // Step 6: Process the prompt
    // Note: KV cache is managed internally by llama_decode
    
    if (llama_decode(pImpl->ctx, batch) != 0) {
        LOGE("Failed to process prompt");
        llama_batch_free(batch);
        if (token_cb) {
            token_cb("", true);
        }
        return;
    }
    
    // Step 7: Generate tokens with streaming
    int n_cur = batch.n_tokens;
    int n_decode = 0;
    const int n_max_tokens = 800;  // Maximum output tokens
    
    std::string generated_text;
    
    // Reset sampling context
    common_sampler_reset(pImpl->sampling_ctx);
    
    while (n_decode < n_max_tokens && !cancel_flag) {
        // Sample next token using the sampling context
        llama_token new_token_id = common_sampler_sample(
            pImpl->sampling_ctx,
            pImpl->ctx,
            batch.n_tokens - 1,  // Index of last token in batch
            false                // Don't apply grammar first
        );
        
        // Accept the sampled token
        common_sampler_accept(pImpl->sampling_ctx, new_token_id, true);
        
        // Check for end of generation
        const llama_vocab* vocab_eos = llama_model_get_vocab(pImpl->model);
        if (new_token_id == llama_vocab_eos(vocab_eos)) {
            LOGD("EOS token reached");
            break;
        }
        
        // Convert token to text
        const llama_vocab* vocab2 = llama_model_get_vocab(pImpl->model);
        char token_str[256];
        int token_len = llama_token_to_piece(
            vocab2,
            new_token_id,
            token_str,
            sizeof(token_str),
            0,
            true  // special
        );
        
        if (token_len > 0) {
            std::string token_text(token_str, token_len);
            generated_text += token_text;
            
            // Check for completion marker
            if (generated_text.find("### End") != std::string::npos) {
                LOGD("Completion marker found");
                // Remove the marker from output
                size_t marker_pos = generated_text.find("### End");
                generated_text = generated_text.substr(0, marker_pos);
                if (token_cb && !generated_text.empty()) {
                    token_cb(generated_text, false);
                    generated_text.clear();
                }
                break;
            }
            
            // Stream token to callback
            if (token_cb) {
                token_cb(token_text, false);
            }
        }
        
        // Token is already tracked by sampling context
        
        // Prepare next batch
        batch.n_tokens = 0;  // Clear the batch
        common_batch_add(batch, new_token_id, n_cur, {0}, true);
        n_cur++;
        
        // Decode next token
        if (llama_decode(pImpl->ctx, batch) != 0) {
            LOGE("Failed to decode token %d", n_decode);
            break;
        }
        
        n_decode++;
    }
    
    // Clean up
    llama_batch_free(batch);
    
    // Send final callback
    if (token_cb && !cancel_flag) {
        token_cb("", true);
    }
    
    LOGD("Text processing complete - generated %d tokens", n_decode);
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