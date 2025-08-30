# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## CRITICAL: Source of Truth

**The Product Requirements Document (PRD) at `_docs/PRD.md` is the authoritative source of truth for this project.** All implementation decisions must align with the PRD.

## Project Overview

Crispify is an Android text simplification app using on-device LLM inference. It rewrites complex text to a 7th-grade reading level while preserving all key information. Privacy is guaranteed through no internet permission.

## Current Model Configuration

- **Model:** Gemma-3 270M IT QAT Q5_K_M from https://huggingface.co/unsloth/gemma-3-270m-it-qat-GGUF
- **Location:** `app/src/main/assets/models/gemma-3-270m-it-qat-Q5_K_M.gguf`
- **Inference:** llama.cpp via JNI with adaptive prompt engineering based on text length

## Build Commands

```bash
./gradlew assembleDebug    # Build debug APK
./gradlew installDebug     # Install on device
./gradlew test             # Run unit tests
./gradlew lint             # Run lint checks
./gradlew clean            # Clean build
```

## Key Architecture

```
app/src/main/
├── java/com/clickapps/crispify/
│   ├── ProcessTextActivity.kt      # ACTION_PROCESS_TEXT handler
│   ├── engine/
│   │   ├── LlamaEngine.kt         # Main inference engine
│   │   └── ModelAssetManager.kt   # GGUF model extraction
│   └── ui/process/
│       └── ProcessTextViewModel.kt # UI state management
└── cpp/
    ├── llama_wrapper.cpp          # Core inference with adaptive prompting
    ├── crispify_jni.cpp          # JNI bridge
    └── CMakeLists.txt            # Native build config
```

## Adaptive Prompt System (PR #14)

The native layer (`llama_wrapper.cpp`) implements adaptive prompting:
- **Short texts (<25 words):** 1-2 sentence outputs
- **Medium texts (25-75 words):** 2-3 sentence balanced simplification  
- **Long texts (>75 words):** 3-4 sentence key information extraction

## Implementation Status

### ✅ Completed
- ProcessTextActivity with Material 3 bottom sheet
- First Launch onboarding with model initialization
- Full llama.cpp integration with JNI callbacks
- Token streaming from native to UI
- Adaptive prompt engineering (PR #14)
- Chat template support for model formatting
- Dynamic batch sizing to prevent crashes

### 🚧 Pending
- Google Play Asset Delivery (currently bundled in APK)
- Production device testing
- Remove debug logging before release

## Testing

Known issues:
- Android Log mocking prevents some unit tests from running
- See `.agent-os/specs/2025-08-29-model-inference-llama-cpp/testing-issues.md` for details

## PRD Constraints

- **Single Feature:** Text leveling only (no summary/jargon modes)
- **Token Limit:** ~1200 tokens max input
- **No State:** App doesn't remember preferences
- **Copy Only:** No text replacement functionality

## Error Messages

| Condition | User Message |
|-----------|--------------|
| Input too long | "Please select a smaller amount of text for this version." |
| Engine failure | "An error occurred. Please try again." |
| Out of memory | "Not enough memory to process this text." |

## Agent OS Workflow

Development happens on feature branches:
1. Create branch: `git checkout -b feature-name`
2. Make changes and test
3. Commit with detailed message
4. Push and create PR for review
5. Never commit directly to main

## Helper Scripts

For Kotlin symbol search (preferred over tree-sitter):
```bash
./.scripts/kotlin-symbols.sh MainActivity
./.scripts/kotlin-symbols.sh LlamaEngine
```