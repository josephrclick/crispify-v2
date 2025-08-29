# Spec Tasks

These are the tasks to be completed for the spec detailed in @.agent-os/specs/2025-08-29-model-inference-llama-cpp/spec.md

> Created: 2025-08-29
> Status: Ready for Implementation

## Tasks

- [x] 1. Implement Core Inference Engine
  - [x] 1.1 Write tests for prompt template construction
  - [x] 1.2 Implement buildPrompt() function with system preface and user template
  - [ ] 1.3 Write tests for tokenization and token limit validation
  - [x] 1.4 Implement tokenization with 1000 token input limit check
  - [x] 1.5 Replace stub processText() with llama.cpp inference calls
  - [ ] 1.6 Verify all inference tests pass

- [x] 2. Implement Token Streaming and Generation
  - [ ] 2.1 Write tests for token-by-token generation
  - [x] 2.2 Configure sampling parameters (temperature, top-p, top-k, repetition)
  - [x] 2.3 Implement sampling chain in correct order
  - [x] 2.4 Add token streaming through existing TokenCallback
  - [x] 2.5 Implement completion detection for "### End" marker and EOS
  - [ ] 2.6 Verify streaming tests pass

- [x] 3. Implement Error Handling and Memory Management
  - [ ] 3.1 Write tests for error code propagation
  - [x] 3.2 Define InferenceError enum with all error codes
  - [x] 3.3 Implement memory tracking using llama.cpp native functions
  - [ ] 3.4 Add pre-flight memory availability check
  - [x] 3.5 Implement error code mapping to JNI layer
  - [ ] 3.6 Verify error handling tests pass

- [x] 4. Optimize for Mobile Performance
  - [ ] 4.1 Write performance benchmark tests
  - [x] 4.2 Configure batch size to 128 for mobile optimization
  - [x] 4.3 Implement efficient buffer management for streaming
  - [x] 4.4 Add cancellation support via cancel_flag
  - [ ] 4.5 Verify performance meets <2 second target
  - [ ] 4.6 Verify all optimization tests pass

- [ ] 5. Integration Testing and Cleanup
  - [ ] 5.1 Write end-to-end integration tests
  - [ ] 5.2 Test with actual GGUF model on device
  - [ ] 5.3 Remove debug logging for privacy compliance
  - [ ] 5.4 Verify memory cleanup and no leaks
  - [ ] 5.5 Run full test suite and verify all tests pass