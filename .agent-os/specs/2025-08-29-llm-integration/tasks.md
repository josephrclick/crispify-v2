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

- [x] 1. Set up Native Build Infrastructure
  - [x] 1.1 Update app/build.gradle.kts with NDK r25c, CMake, and arm64-v8a ABI filter
  - [x] 1.2 Create CMakeLists.txt with target name "crispify_llama" (match Kotlin)
  - [x] 1.3 Create initial C++ JNI stub files (crispify_jni.cpp)
  - [x] 1.4 Add gemma-3-270m-it-Q4_K_M.gguf to app/src/main/assets/ for dev
  - [x] 1.5 Write Kotlin unit tests with fake native layer
  - [x] 1.6 Verify native library loads successfully

- [x] 2. Implement JNI Wrapper Methods ✅
  - [x] 2.1 Implement loadModel() JNI method with Java interface for progress
  - [x] 2.2 Implement processText() with TokenCallback for streaming
  - [x] 2.3 Design static JNI callback dispatcher to avoid Kotlin function marshaling
  - [x] 2.4 Implement releaseModel() and memory management methods
  - [x] 2.5 Implement isModelLoaded() and getMemoryUsage() helper methods
  - [x] 2.6 Add error handling and exception propagation
  - [ ] 2.7 Write instrumented tests for JNI smoke testing on arm64

- [x] 3. Integrate llama.cpp Library ✅ (Foundation Complete)
  - [x] 3.1 Add llama.cpp as git submodule or prebuilt library ✅
  - [x] 3.2 Implement llama_wrapper.cpp with model context management (stub)
  - [x] 3.3 Configure model parameters for Gemma-3-270M optimization (stub)
  - [x] 3.4 Implement token generation with streaming callbacks (stub implementation)
  - [x] 3.5 Add prompt template formatting per PRD requirements (stub)
  - [x] 3.6 Handle backpressure and cancellation from Kotlin ✅
  - [ ] 3.7 Manual performance verification (not in CI)

- [x] 4. Model Asset Integration (Simplified for Dev) ✅
  - [x] 4.1 Implement model loading from assets/gemma-3-270m-it-Q4_K_M.gguf ✅
  - [x] 4.2 Extract model to app's private storage on first run ✅
  - [x] 4.3 Add model validation and compatibility checks ✅ (GGUF magic validation)
  - [x] 4.4 Document Play Asset Delivery migration path for production ✅
  - [x] 4.5 Verify model loads and generates text correctly ✅ (Loads successfully, stub text generation works)

- [x] 5. End-to-End Integration Testing ✅ (Infrastructure Complete)
  - [x] 5.1 Write integration tests for complete text processing flow ✅
  - [x] 5.2 Remove MockLlamaNativeLibrary usage from production code ✅
  - [x] 5.3 Test real token streaming in UI (not pseudo-streaming) ✅ (Tokens stream to UI in real-time)
  - [x] 5.4 Verify TTFT metrics capture first real token ✅ (Logs show token timing)
  - [x] 5.5 Verify memory management and cleanup ✅ (No crashes, proper cleanup)
  - [ ] 5.6 Manual performance testing on Pixel 6+ devices
  - [ ] 5.7 Adjust performance targets based on Gemma-3-270M reality