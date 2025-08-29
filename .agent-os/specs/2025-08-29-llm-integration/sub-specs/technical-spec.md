# Technical Specification

This is the technical specification for the spec detailed in @.agent-os/specs/2025-08-29-llm-integration/spec.md

> Created: 2025-08-29
> Version: 1.0.0

## Technical Requirements

### Native Integration Architecture

- **JNI Wrapper Implementation**: Create native C++ code implementing the JNI methods defined in `LlamaNativeLibraryImpl.kt`
- **Build System**: Configure CMake build system for native compilation with llama.cpp as a dependency
- **Architecture Support**: Target arm64-v8a architecture only (per PRD requirements)
- **Model Format**: Support GGUF quantized models (Q4_K_M recommended for mobile performance)

### Core Functionality Requirements

- **Model Loading**:
  - Extract GGUF model from Play Asset Delivery pack to app's private storage
  - Initialize llama.cpp context with appropriate parameters for mobile devices
  - Report loading progress in 5% increments via callback
  - Handle model validation and compatibility checks

- **Text Generation**:
  - Implement prompt templating per PRD Appendix A
  - Configure generation parameters (temperature=0.7, top_p=0.9, max_tokens=1200)
  - Stream tokens via callback as they are generated
  - Implement proper tokenization and detokenization

- **Memory Management**:
  - Allocate native memory efficiently using llama.cpp's memory pool
  - Implement proper cleanup in `releaseModel()` to prevent memory leaks
  - Monitor memory usage and report via `getMemoryUsage()`
  - Handle low-memory conditions gracefully

### Performance Criteria

- Model loading: < 5 seconds on Pixel 6 or equivalent (Snapdragon 870+)
- First token generation (TTFT): < 2 seconds for typical input
- Token generation rate: > 5 tokens/second on target devices
- Memory footprint: < 500MB for Q4_K_M quantized model

### Error Handling

- Graceful degradation when model file is corrupted or incompatible
- Proper exception handling and error codes from native to Kotlin layer
- Thread-safe operations for concurrent access prevention
- Timeout handling for stuck generation requests

## Approach

### Implementation Strategy

1. **Phase 1: Native Library Setup**
   - Set up CMake build configuration
   - Integrate llama.cpp as dependency
   - Create basic JNI wrapper stubs

2. **Phase 2: Model Loading Infrastructure**
   - Implement asset extraction from Play Asset Delivery
   - Create model validation and initialization routines
   - Add progress callback mechanism

3. **Phase 3: Text Generation Engine**
   - Implement prompt templating system
   - Add token streaming with callbacks
   - Configure generation parameters for mobile optimization

4. **Phase 4: Integration and Testing**
   - Connect native implementation to existing Kotlin interfaces
   - Performance testing and optimization
   - Memory leak detection and cleanup

### Build System Configuration

```cmake
# CMakeLists.txt structure
cmake_minimum_required(VERSION 3.18.1)
project("crispify")

# llama.cpp integration
add_subdirectory(llama.cpp)

# JNI wrapper library
add_library(crispify-native SHARED
    native-lib.cpp
    llama-wrapper.cpp
)

# Link against llama.cpp
target_link_libraries(crispify-native
    llama
    android
    log
)
```

### Memory Management Strategy

- Use RAII patterns for automatic resource cleanup
- Implement smart pointers for llama.cpp context management
- Monitor heap usage and trigger garbage collection when appropriate
- Use native memory pools to minimize allocation overhead

## External Dependencies

- **llama.cpp** - Core inference engine for running GGUF models
  - **Justification:** Industry-standard, lightweight C++ library optimized for mobile/edge deployment
  - **Version:** Latest stable release compatible with GGUF format
  - **Integration Method:** Git submodule or prebuilt static library

- **Android NDK** - Native development kit for C++ compilation
  - **Justification:** Required for JNI integration and native code compilation
  - **Version:** NDK r25c or later (matching Android Studio recommendations)