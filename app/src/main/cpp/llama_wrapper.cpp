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
    params.temp = 0.45f;             // Tighter for small IT models
    params.top_p = 0.9f;             // Nucleus sampling
    params.top_k = 30;               // Top-k filtering
    params.penalty_repeat = 1.10f;   // Reduce repetition/echo
    params.penalty_last_n = 256;     // Longer lookback for repetition
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
    // Chat template (built-in, from model)
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

    // Diagnostics: log model description and chat template (if any)
    char model_desc[256] = {0};
    llama_model_desc(pImpl->model, model_desc, sizeof(model_desc));
    LOGD("Model description: %s", model_desc);
    // Initialize chat templates from model (if available)
    pImpl->chat_templates = common_chat_templates_init(pImpl->model, /*override*/ "");
    if (pImpl->chat_templates) {
        const char* src = common_chat_templates_source(pImpl->chat_templates.get(), nullptr);
        LOGD("Model chat template detected (source: %s)", src ? src : "unknown");
    } else {
        LOGD("Model chat template: none");
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
            token_cb("", true); // Signal error with InferenceError::OUT_OF_MEMORY
        }
        return;
    }
    
    // Step 1: Format messages via the model's built-in chat template
    // Build system and user messages for leveling task
    const std::string sys_msg =
        "You are an expert editor who simplifies complex text. "
        "Follow instructions precisely. Your output must be clear, factual, and easy to read. "
        "Write only the simplified version of the text. Do not repeat the instructions or the original text. "
        "Keep all key facts, names, and numbers. Use shorter sentences and simple words. "
        "Do not include headings or markdown in your output.";

    std::string user_msg =
        "Rewrite the following text in clear, plain language suitable for a 7th-grade reading level. "
        "Use shorter sentences and simple words. Do not add new information or opinions.\n\n"
        "Original Text:\n" + input_text;

    std::string full_prompt;
    std::vector<std::string> additional_stops;
    if (pImpl->chat_templates) {
        common_chat_templates_inputs inputs;
        // default: add_generation_prompt = true
        inputs.use_jinja = true;
        inputs.messages.push_back({"system", sys_msg});
        inputs.messages.push_back({"user",   user_msg});
        auto chat_params = common_chat_templates_apply(pImpl->chat_templates.get(), inputs);
        full_prompt = chat_params.prompt;
        additional_stops = chat_params.additional_stops;
    } else {
        // Fallback: concatenate messages if no chat template is available
        full_prompt = sys_msg + "\n\nUser:\n" + user_msg + "\n\nAssistant:";
    }

    // Step 2: Tokenize chat-formatted prompt with special token parsing
    std::vector<llama_token> prompt_tokens = common_tokenize(pImpl->ctx, full_prompt, /*add_special=*/false, /*parse_special=*/true);
    const int n_prompt_tokens = (int) prompt_tokens.size();

    // Enforce spec total limit (~1200 tokens total including template)
    if (n_prompt_tokens > 1200) {
        LOGE("Total prompt exceeds 1200 tokens: %d", n_prompt_tokens);
        if (token_cb) token_cb("", true);
        return;
    }
    
    // Ensure we do not exceed context window
    const int n_ctx = (int) llama_n_ctx(pImpl->ctx);
    const int n_batch_dbg = (int) llama_n_batch(pImpl->ctx);
    // Augment with conservative manual stops to avoid verbose patterns
    additional_stops.push_back("\nAnswer:");
    additional_stops.push_back("Final Answer:");
    additional_stops.push_back("Here's why:");
    LOGD("Diagnostics: n_ctx=%d, n_batch=%d, total_prompt_tokens=%d", n_ctx, n_batch_dbg, n_prompt_tokens);
    LOGD("Stops configured: %zu", additional_stops.size());
    if (n_prompt_tokens > n_ctx) {
        LOGE("Prompt exceeds context size (%d > %d)", n_prompt_tokens, n_ctx);
        if (token_cb) token_cb("", true);
        return;
    }
    
    // Step 4: Initialize batch for processing
    // Important: llama_decode expects batch.n_tokens <= llama_n_batch(ctx)
    const int n_batch_ctx = (int) llama_n_batch(pImpl->ctx);
    llama_batch batch = llama_batch_init(n_batch_ctx, 0, 1);
    
    // Ingest the prompt in chunks of n_batch_ctx
    const int n_chunks = (n_prompt_tokens + n_batch_ctx - 1) / n_batch_ctx;
    LOGD("Prompt ingestion: n_prompt_tokens=%d, n_batch=%d, n_chunks=%d", n_prompt_tokens, n_batch_ctx, n_chunks);
    for (int i = 0; i < n_prompt_tokens; ) {
        common_batch_clear(batch);
        const int n_this = std::min(n_batch_ctx, n_prompt_tokens - i);
        LOGD("Prompt chunk: start=%d, size=%d", i, n_this);
        for (int j = 0; j < n_this; ++j) {
            const bool is_last_overall = (i + j == n_prompt_tokens - 1);
            common_batch_add(batch, prompt_tokens[i + j], i + j, {0}, is_last_overall);
        }
        if (llama_decode(pImpl->ctx, batch) != 0) {
            LOGE("Failed to process prompt chunk at i=%d (n_this=%d)", i, n_this);
            llama_batch_free(batch);
            if (token_cb) token_cb("", true);
            return;
        }
        i += n_this;
    }
    
    // Step 7: Generate tokens with streaming
    // After prompt ingestion, current position equals number of prompt tokens
    int n_cur = n_prompt_tokens;
    int n_decode = 0;
    const int n_max_tokens = 256;  // Maximum output tokens (tuned for leveling)
    
    std::string generated_text;
    
    // Reset sampling context
    common_sampler_reset(pImpl->sampling_ctx);
    
    const auto gen_start = std::chrono::steady_clock::now();
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
            LOGD("EOS token reached (id=%d)", (int) new_token_id);
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
            // Preview new text with this token before streaming
            std::string preview = generated_text;
            preview += token_text;

            // Legacy completion marker (should no longer be present)
            if (preview.find("### End") != std::string::npos) {
                LOGD("Completion marker found");
                break;
            }
            // Stop on chat/template-provided or manual stop strings
            bool stop_hit = false;
            for (const auto & s : additional_stops) {
                if (!s.empty() && preview.find(s) != std::string::npos) {
                    LOGD("Stop string hit: %s", s.c_str());
                    stop_hit = true;
                    break;
                }
            }
            if (stop_hit) {
                break;
            }
            // Stop if model starts echoing prompt sections
            if (preview.find("### Simplified Text") != std::string::npos ||
                preview.find("Original Text:") != std::string::npos) {
                LOGD("Stop: detected prompt echo in generated output");
                break;
            }

            // Accept and stream
            generated_text = preview;
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
        if (n_decode % 50 == 0) {
            LOGD("Generated tokens so far: %d", n_decode);
        }
    }
    
    // Clean up
    llama_batch_free(batch);
    
    // Send final callback
    if (token_cb && !cancel_flag) {
        token_cb("", true);
    }
    
    const auto gen_end = std::chrono::steady_clock::now();
    const auto gen_ms = std::chrono::duration_cast<std::chrono::milliseconds>(gen_end - gen_start).count();
    double tps = gen_ms > 0 ? (n_decode * 1000.0) / (double) gen_ms : 0.0;
    if (cancel_flag) {
        LOGD("Text processing cancelled after %d tokens", n_decode);
    }
    LOGD("Text processing complete - generated %d tokens in %lld ms (%.2f tok/s)", n_decode, (long long) gen_ms, tps);
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
