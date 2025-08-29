# Spec Tasks

## Pre-Work: API Design Decisions (MUST DO FIRST)

- [x] 0. Define Streaming API and Align Components
  - [x] 0.1 Update LlamaNativeLibrary interface to support token streaming callback
  - [x] 0.2 Replace processText(String): String with processText(String, TokenCallback)
  - [x] 0.3 Define TokenCallback as Java SAM interface for JNI compatibility
  - [x] 0.4 Update ProcessTextViewModel to consume real token stream (not pseudo)
  - [x] 0.5 Fix TTFT calculation to measure first token arrival
  - [x] 0.6 Document threading model and cancellation mechanism

## Tasks

- [ ] 1. Set up Native Build Infrastructure
  - [ ] 1.1 Update app/build.gradle.kts with NDK r25c, CMake, and arm64-v8a ABI filter
  - [ ] 1.2 Create CMakeLists.txt with target name "crispify_llama" (match Kotlin)
  - [ ] 1.3 Create initial C++ JNI stub files (crispify_jni.cpp)
  - [ ] 1.4 Add gemma-3-270m-it-Q4_K_M.gguf to app/src/main/assets/ for dev
  - [ ] 1.5 Write Kotlin unit tests with fake native layer
  - [ ] 1.6 Verify native library loads successfully

- [ ] 2. Implement JNI Wrapper Methods
  - [ ] 2.1 Implement loadModel() JNI method with Java interface for progress
  - [ ] 2.2 Implement processText() with TokenCallback for streaming
  - [ ] 2.3 Design static JNI callback dispatcher to avoid Kotlin function marshaling
  - [ ] 2.4 Implement releaseModel() and memory management methods
  - [ ] 2.5 Implement isModelLoaded() and getMemoryUsage() helper methods
  - [ ] 2.6 Add error handling and exception propagation
  - [ ] 2.7 Write instrumented tests for JNI smoke testing on arm64

- [ ] 3. Integrate llama.cpp Library
  - [ ] 3.1 Add llama.cpp as git submodule or prebuilt library
  - [ ] 3.2 Implement llama_wrapper.cpp with model context management
  - [ ] 3.3 Configure model parameters for Gemma-3-270M optimization
  - [ ] 3.4 Implement token generation with streaming callbacks
  - [ ] 3.5 Add prompt template formatting per PRD requirements
  - [ ] 3.6 Handle backpressure and cancellation from Kotlin
  - [ ] 3.7 Manual performance verification (not in CI)

- [ ] 4. Model Asset Integration (Simplified for Dev)
  - [ ] 4.1 Implement model loading from assets/gemma-3-270m-it-Q4_K_M.gguf
  - [ ] 4.2 Extract model to app's private storage on first run
  - [ ] 4.3 Add model validation and compatibility checks
  - [ ] 4.4 Document Play Asset Delivery migration path for production
  - [ ] 4.5 Verify model loads and generates text correctly

- [ ] 5. End-to-End Integration Testing
  - [ ] 5.1 Write integration tests for complete text processing flow
  - [ ] 5.2 Remove MockLlamaNativeLibrary usage from production code
  - [ ] 5.3 Test real token streaming in UI (not pseudo-streaming)
  - [ ] 5.4 Verify TTFT metrics capture first real token
  - [ ] 5.5 Verify memory management and cleanup
  - [ ] 5.6 Manual performance testing on Pixel 6+ devices
  - [ ] 5.7 Adjust performance targets based on Gemma-3-270M reality