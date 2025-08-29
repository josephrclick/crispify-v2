#ifndef LLAMA_WRAPPER_H
#define LLAMA_WRAPPER_H

#include <string>
#include <functional>
#include <atomic>

/**
 * Wrapper class for llama.cpp integration
 * Manages model loading, text generation, and resource cleanup
 */
class LlamaWrapper {
public:
    // Callback types
    using ProgressCallback = std::function<void(float)>;
    using TokenCallback = std::function<void(const std::string&, bool)>;
    
    LlamaWrapper();
    ~LlamaWrapper();
    
    // Delete copy constructor and assignment
    LlamaWrapper(const LlamaWrapper&) = delete;
    LlamaWrapper& operator=(const LlamaWrapper&) = delete;
    
    /**
     * Load GGUF model from file
     * @param model_path Path to the model file
     * @param progress_cb Progress callback (0.0 to 1.0)
     * @return true if loaded successfully
     */
    bool loadModel(const std::string& model_path, ProgressCallback progress_cb);
    
    /**
     * Process text through the model with token streaming
     * @param input_text Text to process
     * @param token_cb Token callback for streaming
     * @param cancel_flag Atomic flag for cancellation
     */
    void processText(const std::string& input_text, 
                    TokenCallback token_cb,
                    const std::atomic<bool>& cancel_flag);
    
    /**
     * Release model and free resources
     */
    void releaseModel();
    
    /**
     * Check if model is loaded
     */
    bool isModelLoaded() const;
    
    /**
     * Get current memory usage in bytes
     */
    size_t getMemoryUsage() const;
    
private:
    struct Impl;
    std::unique_ptr<Impl> pImpl;
};

#endif // LLAMA_WRAPPER_H