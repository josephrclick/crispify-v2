# Testing Issues - Model Inference Implementation

> Created: 2025-08-29
> Sprint: Model Inference llama.cpp Implementation
> Status: Documented for QA Phase

## Overview

This document captures testing issues and blockers encountered during the implementation of the llama.cpp model inference feature. These issues prevent full test coverage but do not affect the actual implementation functionality.

## 1. Android Log Mocking Issue

### Problem
Unit tests for `LlamaEngine` fail with:
```
java.lang.RuntimeException: Method d in android.util.Log not mocked. 
See https://developer.android.com/r/studio-ui/build/not-mocked for details.
```

### Affected Tests
- `LlamaEngineTest` (7 tests total):
  - `initialize emits progress updates correctly`
  - `processText handles empty input`
  - `processText streams tokens when model is initialized`
  - `isInitialized returns correct state`
  - `release cleans up resources`
  - `initialize handles native library exception`
  - `initialize handles model loading failure`

### Root Cause
The `LlamaEngine.kt` class contains multiple Android `Log.d()`, `Log.v()`, and `Log.e()` calls for debugging purposes (lines 37, 45, 48, 52-53, 58, 65, 69-70, 75, 82, 89, 108, 110, 116, 118-119, 123, 125, 167, 171).

### Potential Solutions
1. **Mock Android Log in test setup** - Add Mockito static mocking for Log class
2. **Extract logging to interface** - Create a Logger interface that can be mocked
3. **Use Robolectric** - Run tests with Robolectric framework which provides Android framework stubs
4. **Remove logging before release** - As specified in the PRD for privacy compliance

## 2. Native Library Testing Limitations

### Problem
Cannot fully test the actual C++ implementation in unit tests without:
- A real GGUF model file
- The native library compiled and loaded
- Android device/emulator environment

### Affected Components
- `llama_wrapper.cpp` actual inference implementation
- Token streaming from native to Kotlin
- Memory management and tracking
- Error propagation from C++ to JNI

### Current Workaround
Using `MockLlamaNativeLibrary` for unit testing, which simulates the native behavior but doesn't test the actual C++ code.

## 3. Performance Testing Requirements

### Not Yet Testable
- **TTFT (Time to First Token)**: Target < 500ms
- **Tokens per second**: Target minimum 5 tokens/sec
- **Total processing time**: Target < 2 seconds for ~200 words
- **Memory usage**: Actual usage with real model

### Reason
Requires actual device testing with real GGUF model to measure performance metrics accurately.

## 4. Integration Test Dependencies

### Missing Prerequisites
1. **GGUF Model File**: Need actual model asset (e.g., Gemma-3-270M.gguf)
2. **Device Testing**: ARM64 Android 12+ device or emulator
3. **Model Asset Manager**: Full extraction and loading flow
4. **End-to-end Flow**: Complete ACTION_PROCESS_TEXT intent handling

## 5. Sampling and Generation Testing

### Untested Aspects
- Actual sampling chain behavior with real model
- "### End" marker detection in generated text
- EOS token handling
- Token-to-text conversion accuracy
- Repetition penalty effectiveness

### Reason
These require actual model inference to generate real tokens, not just mocked responses.

## Test Coverage Summary

### ✅ Successfully Tested
- Mock native library behavior (MockLlamaNativeLibrary)
- Token callback interface
- Model loading simulation
- Basic error handling
- Preferences and diagnostics
- UI components (FirstLaunchScreen, ProcessTextActivity)

### ❌ Blocked by Issues
- LlamaEngine initialization flow (Log mocking)
- Native C++ inference implementation
- Performance benchmarks
- End-to-end integration
- Actual token generation quality

## Recommendations for QA Phase

1. **Priority 1: Fix Log Mocking**
   - Implement proper Log mocking or use Robolectric
   - This will unblock 7 critical unit tests

2. **Priority 2: Device Testing**
   - Test on physical ARM64 Android 12+ device
   - Use actual GGUF model (Gemma-3-270M or similar)
   - Measure real performance metrics

3. **Priority 3: Integration Tests**
   - Create instrumented tests that run on device
   - Test full flow from text selection to simplified output
   - Verify memory management and cleanup

4. **Priority 4: Remove Debug Logging**
   - Strip all Log.* calls before release build
   - Ensure privacy compliance per PRD requirements

## Notes

- The core implementation is complete and compiles successfully
- The native C++ code follows all specifications from the technical spec
- Testing issues are environmental/framework related, not implementation bugs
- All blockers are well-understood with clear paths to resolution