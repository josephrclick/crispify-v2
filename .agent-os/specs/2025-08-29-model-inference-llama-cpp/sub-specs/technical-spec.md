# Technical Specification

This is the technical specification for the spec detailed in @.agent-os/specs/2025-08-29-model-inference-llama-cpp/spec.md

> Created: 2025-08-29
> Version: 1.0.0

## Technical Requirements

### Core Inference Implementation

- **File to Modify**: `app/src/main/cpp/llama_wrapper.cpp` (lines 98-133)
- **Replace stub `processText()` method** with actual llama.cpp inference calls
- **Implement prompt template injection** per PRD Appendix A specification
- **Configure sampling parameters**: temperature ~0.7, top-p ~0.9, top-k ~40
- **Enable token-by-token streaming** through existing `TokenCallback` mechanism
- **Implement completion detection** for "### End" marker or EOS token

### Token Processing

- **Input tokenization** using `llama_tokenize()` API
- **Token limit validation** - enforce ~1000 user input token maximum (reserving ~200 for prompt template)
- **Pre-flight token counting**: Count full prompt (system preface + template + user input) before inference
- **Total context validation**: Ensure complete prompt stays under 1200 tokens total
- **Output generation** - limit to ~800 output tokens
- **Token-to-text conversion** using `llama_token_to_piece()`

### Memory Management

- **Context size**: 2048 tokens (already configured)
- **Batch size optimization**: Use 128 tokens for mobile devices (reduced from 512)
- **Batch processing** using `llama_batch_init(128, 0, 1)` with proper cleanup
- **Memory tracking** using llama.cpp native functions:
  ```cpp
  // Get actual model memory usage
  size_t model_size = llama_model_size(model);
  size_t context_size = llama_state_get_size(ctx);
  pImpl->memory_usage = model_size + context_size;
  ```
- **Efficient buffer management** for token streaming with pre-allocated buffers
- **Memory monitoring**: Check available memory before inference starts

### Error Handling

- **Error Code Enum Definition**:
  ```cpp
  enum class InferenceError {
      NONE = 0,
      TOKEN_LIMIT_EXCEEDED = 1,    // Input exceeds 1000 tokens
      INFERENCE_FAILED = 2,         // llama.cpp decode error
      OUT_OF_MEMORY = 3,           // Memory allocation failed
      MODEL_NOT_LOADED = 4,        // Model not initialized
      CONTEXT_OVERFLOW = 5,        // Total prompt exceeds context
      CANCELLED = 6                // User cancelled operation
  };
  ```
- **Token limit exceeded**: Return `TOKEN_LIMIT_EXCEEDED` for UI message mapping
- **Inference failure**: Return `INFERENCE_FAILED` with llama.cpp error details
- **Out of memory**: Return `OUT_OF_MEMORY` on allocation failures
- **Model not loaded**: Return `MODEL_NOT_LOADED` if model not initialized
- **JNI Error Propagation**: Pass error code through existing callback mechanism

### Integration Points

- **JNI Layer**: Use existing `crispify_jni.cpp` callbacks (no changes needed)
- **Progress Callbacks**: Utilize existing `TokenCallback` for streaming
- **Cancellation**: Respect `cancel_flag` atomic boolean for user interruption
- **Model Loading**: Leverage existing `loadModel()` implementation

### Performance Targets

- **Time to First Token (TTFT)**: < 500ms for typical input
- **Tokens per second**: Minimum 5 tokens/sec on mid-range devices
- **Total processing time**: < 2 seconds for typical paragraphs (~200 words)
- **CPU threads**: Use configured 4 threads for optimal performance
- **Batch configuration for mobile**:
  ```cpp
  // Optimized for mobile devices (reduced memory footprint)
  llama_context_params ctx_params = llama_context_default_params();
  ctx_params.n_ctx = 2048;     // Context window
  ctx_params.n_batch = 128;    // Reduced from 512 for mobile
  ctx_params.n_ubatch = 128;   // Physical batch size
  ctx_params.n_threads = 4;    // CPU threads
  ctx_params.n_threads_batch = 4; // Batch processing threads
  ```

### llama.cpp API Usage

```cpp
// Key APIs to implement:
llama_tokenize()           // Convert text to tokens
llama_batch_init()         // Initialize batch for processing
llama_batch_add()          // Add tokens to batch
llama_decode()             // Process input tokens

// Sampling chain (apply in this order):
llama_sample_repetition_penalties()  // Apply rep penalty (1.05)
llama_sample_top_k()                 // Top-k sampling (k=40)
llama_sample_top_p()                 // Top-p sampling (p=0.9)
llama_sample_temp()                  // Temperature (t=0.7)
llama_sample_token()                 // Final token selection

llama_token_to_piece()     // Convert tokens back to text
llama_batch_free()         // Clean up batch
```

### Sampling Configuration

```cpp
// Sampling parameters for text simplification
struct SamplingParams {
    float temperature = 0.7f;      // Balanced creativity
    float top_p = 0.9f;            // Nucleus sampling
    int32_t top_k = 40;            // Top-k filtering
    float repeat_penalty = 1.05f;  // Slight repetition penalty
    int32_t repeat_last_n = 64;    // Lookback for repetition
};
```

## Approach

### Implementation Strategy

The implementation will transform the current stub implementation in `llama_wrapper.cpp` into a fully functional inference engine while maintaining all existing interfaces and error handling patterns.

### Phase 1: Core Inference Loop

1. **Prompt Construction**: 
   - Build complete prompt with exact formatting:
   ```cpp
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
   ```
2. **Tokenization**: Convert complete prompt to tokens and validate limits
3. **Context Preparation**: Initialize llama.cpp batch with size 128 for mobile
4. **Generation Loop**: Implement token-by-token generation with sampling chain
5. **Streaming**: Send each generated token through existing callback system
6. **Completion Detection**: Check for "### End" marker (literal string) or EOS token

### Phase 2: Error Handling and Validation

1. **Input Validation**: Check token limits before processing
2. **Memory Monitoring**: Track allocation failures and context overflow
3. **Completion Detection**: Recognize "### End" marker or EOS tokens
4. **Graceful Degradation**: Handle partial generations and timeouts

### Phase 3: Performance Optimization

1. **Threading**: Utilize existing 4-thread configuration
2. **Memory Efficiency**: Optimize token buffers and batch sizes
3. **Caching**: Leverage llama.cpp's KV cache for efficiency
4. **Profiling**: Measure TTFT and tokens/sec for diagnostics

## External Dependencies

### llama.cpp Integration

- **Version**: Already integrated as git submodule at `third_party/llama.cpp`
- **Build System**: CMake integration already configured in `app/src/main/cpp/CMakeLists.txt`
- **Headers**: `llama.h` and related headers already available
- **Library**: Static linking already configured for arm64-v8a

### Model Format

- **GGUF Format**: Compatible with existing `ModelAssetManager.kt` extraction
- **Quantization**: F16 or Q4_0 quantization for mobile optimization
- **Size**: Target ~1-2GB model size for reasonable app size and performance

### Sampling Dependencies

- **Built-in Sampling**: Use llama.cpp's native sampling functions
- **No External Libraries**: All sampling logic contained within llama.cpp
- **Parameter Tuning**: Use established parameters for text simplification tasks

### Testing Requirements

- **Unit Tests**: Mock llama.cpp calls for unit testing the wrapper logic
- **Integration Tests**: Test with small test model for CI/CD validation
- **Device Testing**: Manual testing on target Android 12+ devices
- **Performance Benchmarks**: Automated timing measurements for regression detection

## Critical Implementation Notes

### Key Updates from Spec Review

1. **Token Limits**: User input must be validated at 1000 tokens MAX (not 1200) to leave room for the ~200 token prompt template
2. **Batch Size**: Reduced from 512 to 128 for mobile memory optimization
3. **Memory Tracking**: Use `llama_model_size()` and `llama_state_get_size()` for accurate reporting
4. **Error Codes**: Implement the `InferenceError` enum for clean JNI error propagation
5. **Sampling Order**: Must apply in sequence: repetition_penalties → top_k → top_p → temperature
6. **Prompt Format**: The "### End" marker is a literal string (no escaping needed in C++)
7. **Completion Detection**: Check for both "### End" string AND EOS token in generated output