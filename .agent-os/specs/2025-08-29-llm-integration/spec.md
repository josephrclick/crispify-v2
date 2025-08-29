# Spec Requirements Document

> Spec: LLM Integration  
> Created: 2025-08-29

## Overview

Implement native llama.cpp integration via JNI wrapper to enable on-device text simplification. This feature replaces the current mock implementation with a real language model capable of processing text locally without internet connectivity.

## User Stories

### Text Simplification User

As a user, I want to select complex text in any app and have it simplified locally on my device, so that I can understand difficult content without compromising my privacy.

When I select text and choose "Crispify" from the text selection toolbar, the app loads the language model (showing progress during first initialization), processes my text through the model, and displays simplified results token-by-token in real-time. The entire process happens on-device without any network requests, ensuring my selected text remains completely private.

### First-Time User

As a first-time user, I want to see clear progress when the model initializes, so that I understand the app is working and not frozen.

During the first launch or when the model needs to be loaded, I see a progress indicator showing the model loading percentage. This gives me confidence that the initialization is proceeding normally, especially since loading a language model can take several seconds on mobile devices.

## Spec Scope

1. **Native JNI Implementation** - Create C++ JNI wrapper connecting Kotlin to llama.cpp library
2. **Model Loading** - Load GGUF format models from app assets with progress callbacks
3. **Text Generation** - Implement token-by-token text generation with streaming callbacks
4. **Memory Management** - Properly manage native memory allocation and deallocation
5. **Error Handling** - Graceful handling of model loading failures and out-of-memory conditions

## Out of Scope

- Model training or fine-tuning capabilities
- Support for multiple models or model switching
- Network-based model downloads or updates
- Custom prompt templates beyond the PRD-specified leveling prompt
- Model quantization or optimization (use pre-quantized GGUF)
- Background processing or service implementation

## Expected Deliverable

1. Functional llama.cpp JNI integration that successfully loads a GGUF model and generates text
2. Token-by-token streaming visible in the UI during text generation
3. Model initialization completing in under 5 seconds on target devices (Pixel 6+)