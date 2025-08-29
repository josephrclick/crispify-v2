# LLM Engine Threading Model

## Overview

The LLM engine uses a carefully designed threading model to ensure smooth UI responsiveness while performing computationally intensive text processing operations.

## Threading Architecture

### Model Initialization
- **Thread**: IO Dispatcher (Coroutines)
- **Lifecycle**: Performed once during first launch or when explicitly requested
- **Progress Callbacks**: Executed on IO thread, marshaled to UI via Flow emissions
- **Duration**: ~3-5 seconds for GGUF model loading

### Text Processing
- **Thread**: IO Dispatcher (Coroutines) 
- **Token Callbacks**: Executed on JNI callback thread, marshaled to UI via coroutine context
- **Cancellation**: Cooperative via volatile flag checked between tokens
- **Duration**: Variable based on input/output length

## Native Layer Threading

### JNI Callback Dispatcher
The native C++ layer uses a static callback dispatcher pattern to avoid complex Kotlin function marshaling:

1. **Native Side**: 
   - Maintains thread-local storage for callback context
   - Invokes static JNI callback method with instance ID
   - Manages callback queue for token streaming

2. **Kotlin Side**:
   - Static method receives callbacks and routes to appropriate instance
   - TokenCallback SAM interface for clean API
   - Coroutine integration for UI updates

## Cancellation Mechanism

### Cooperative Cancellation
Text processing supports graceful cancellation through:

1. **Kotlin Layer**:
   ```kotlin
   fun cancelProcessing() {
       currentJob?.cancel()        // Cancel coroutine
       llamaEngine.cancelProcessing() // Signal native layer
   }
   ```

2. **Native Layer**:
   - Sets atomic cancellation flag
   - Checks flag between token generations
   - Cleanly exits generation loop
   - Sends final callback with isFinished=true

3. **Cleanup**:
   - Resources properly released
   - Model context preserved for next operation
   - UI state updated to reflect cancellation

## Thread Safety Guarantees

### Initialization
- Single initialization enforced via @Volatile flag
- Thread-safe model loading with mutex protection

### Processing
- One active processing operation at a time
- Previous operations cancelled before starting new ones
- Thread-safe callback delivery via synchronized blocks

### Memory Management
- Model memory allocated on native heap
- Garbage collection pressure minimized
- Explicit release() method for cleanup

## Performance Considerations

### Token Streaming
- Tokens delivered as generated (no buffering)
- UI updates throttled by coroutine dispatcher
- Smooth perceived performance via immediate feedback

### First Token Latency (TTFT)
- Measured from processText() call to first onToken() callback
- Target: < 2 seconds on Pixel 6+ devices
- Includes prompt encoding and initial inference

### Throughput
- Target: > 5 tokens/second sustained
- Memory bandwidth limited on mobile devices
- Optimized for Q4_K_M quantization

## Error Handling

### Thread Boundaries
- Exceptions marshaled across JNI boundary
- Kotlin exceptions wrapped in ModelInitializationException
- Native crashes prevented via defensive coding

### Resource Cleanup
- RAII pattern in C++ for automatic cleanup
- Try-finally blocks in Kotlin for guaranteed release
- Cancellation doesn't leak resources