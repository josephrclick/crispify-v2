#include "llama_wrapper.h"
#include <android/log.h>
#include <thread>
#include <chrono>
#include <sstream>
#include <vector>
#include "llama.h"

#define LOG_TAG "LlamaWrapper"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Implementation details (pImpl pattern for ABI stability)
struct LlamaWrapper::Impl {
    bool model_loaded = false;
    size_t memory_usage = 0;
    
    // llama.cpp context and model
    llama_context* ctx = nullptr;
    llama_model* model = nullptr;
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
    pImpl->model = llama_load_model_from_file(model_path.c_str(), model_params);
    if (!pImpl->model) {
        LOGE("Failed to load model from %s", model_path.c_str());
        return false;
    }
    
    // Progress callback at 50%
    if (progress_cb) progress_cb(0.5f);
    
    // Initialize context parameters
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;  // context size
    ctx_params.n_batch = 512; // batch size for prompt processing
    ctx_params.n_threads = 4; // CPU threads
    
    // Create context
    pImpl->ctx = llama_new_context_with_model(pImpl->model, ctx_params);
    if (!pImpl->ctx) {
        LOGE("Failed to create context");
        llama_free_model(pImpl->model);
        pImpl->model = nullptr;
        return false;
    }
    
    // Progress callback at 90%
    if (progress_cb) progress_cb(0.9f);
    
    pImpl->model_loaded = true;
    pImpl->memory_usage = 100 * 1024 * 1024; // Estimate 100MB for now
    
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
    
    // For now, use a simplified stub implementation
    // The full llama.cpp integration requires more complex setup
    
    // Stub implementation - tokenize and stream back
    std::istringstream stream(input_text);
    std::string word;
    int token_count = 0;
    
    // Simple word-based tokenization for stub
    while (stream >> word && !cancel_flag) {
        if (token_count > 0 && token_cb) {
            token_cb(" ", false);
        }
        
        // Mock simplification
        if (word == "utilize") word = "use";
        else if (word == "implement") word = "make";
        else if (word == "functionality") word = "feature";
        
        if (token_cb) {
            token_cb(word, false);
        }
        
        token_count++;
        
        // Simulate token generation delay
        std::this_thread::sleep_for(std::chrono::milliseconds(20));
    }
    
    // Send final token
    if (token_cb && !cancel_flag) {
        token_cb("", true);
    }
    
    LOGD("Text processing complete - %d tokens", token_count);
}

void LlamaWrapper::releaseModel() {
    LOGD("Releasing model resources");
    
    // Clean up llama.cpp resources
    if (pImpl->ctx) {
        llama_free(pImpl->ctx);
        pImpl->ctx = nullptr;
    }
    
    if (pImpl->model) {
        llama_free_model(pImpl->model);
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