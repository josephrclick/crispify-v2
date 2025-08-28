# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## CRITICAL: Source of Truth

**The Product Requirements Document (PRD) at `_docs/PRD.md` is the authoritative source of truth for this project.** All implementation decisions, feature scope, and architectural choices must align with the PRD. When any ambiguity or conflict arises between this document and the PRD, always defer to the PRD.

Before starting any work:
1. Read the complete PRD at `_docs/PRD.md`
2. Ensure all implementation decisions align with the PRD's goals and non-goals
3. Reference specific PRD sections when making architectural or feature decisions

## Project Overview

Crispify is an Android utility that makes dense text easy to understand. It's a private, on-device tool that proves a small language model can be genuinely useful for simplifying text within a user's daily workflow. The v1.0 release focuses on Core Concept Validation as defined in the PRD.

## Key Architectural Decisions (Per PRD Section 2 & 5)

- **Privacy First**: No android.permission.INTERNET permission - guarantees absolute privacy (PRD §2)
- **Single Feature**: Text Leveling only - rewrites to 7th-9th grade reading level (PRD §5)
- **Single Entry Point**: ACTION_PROCESS_TEXT toolbar integration only (PRD §3)
- **UI**: 100% Jetpack Compose with Material 3 bottom sheet (PRD §5)
- **Platform**: Android 12+ (SDK 31+), arm64-v8a architecture only (PRD §5)
- **LLM Engine**: Native llama.cpp (or equivalent) via JNI wrapper (PRD §5)
- **Model Delivery**: Google Play Asset Delivery as install-time asset pack (PRD §5)

## Build Commands

```bash
# Build the app
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Run lint checks
./gradlew lint

# Run unit tests
./gradlew test
./gradlew testDebugUnitTest  # Debug variant only

# Run instrumented tests on connected device
./gradlew connectedAndroidTest
./gradlew connectedDebugAndroidTest  # Debug variant only

# Install on device
./gradlew installDebug

# Clean build
./gradlew clean
```

## Project Structure

```
app/
├── src/main/java/com/clickapps/crispify/
│   ├── MainActivity.kt          # Entry point (currently template code)
│   ├── di/                      # Dependency injection (empty, to be implemented)
│   ├── engine/                  # LLM engine integration (empty, to be implemented)
│   └── ui/theme/               # Material 3 theme configuration
└── src/main/res/               # Resources (layouts, strings, drawables)
```

## Implementation Status

The project is initialized from Android Studio's "Empty Activity" template. Per PRD Section 3 (v1.0 Release Plan), the following components must be implemented:

1. **ProcessTextActivity**: Handle ACTION_PROCESS_TEXT intent (PRD §4 - Core Process Text Flow)
2. **LLM Engine JNI Bridge**: Native interface to llama.cpp library (PRD §5)
3. **Bottom Sheet UI**: Non-intrusive Compose interface for text simplification (PRD §3)
4. **Model Loading**: Play Asset Delivery integration for GGUF model (PRD §5)
5. **Onboarding Screen**: First launch experience with model preparation and diagnostics opt-in (PRD §4)

## Testing Strategy

- Unit tests: JUnit in `src/test/`
- Instrumented tests: AndroidJUnit in `src/androidTest/`
- Test runner: `androidx.test.runner.AndroidJUnitRunner`

## Important Implementation Constraints (PRD Section 2 - Non-Goals)

Per the PRD's explicitly defined non-goals for v1.0:
- **Single Mode**: Leveling only - Summary and Jargon modes are deferred
- **Token Limit**: ~1200 tokens max input - no Smart Split or chunking (PRD §5)
- **Output**: Token-by-token streaming for responsive feel (PRD §5)
- **Error Handling**: Simple, static messages per PRD Section 7
- **No State**: App will not remember user's last choice or have settings (PRD §2)
- **Copy Only**: No replace functionality - only Copy button available (PRD §2)

## Prompt Template (PRD Appendix A)

The v1.0 implementation uses a single, version-pinned prompt template as specified in PRD Appendix A:

**System Preface (internal):**
You are an expert editor who simplifies complex text. You follow instructions precisely. Your output must be clear, factual, and easy to read. You will end your response with a single line that says: ### End

**Leveling Prompt:**
```
### Simplified Text

Rewrite the following text in clear, plain language suitable for a 7th-grade reading level. Preserve all key facts, names, and numbers. Use shorter sentences and simple words. Do not add any new information or opinions.

Original Text:
{{INPUT}}
```

## Privacy & Diagnostics (PRD Section 6)

- **Absolute Privacy**: AndroidManifest.xml must NOT include android.permission.INTERNET
- **Local Diagnostics**: Opt-in only via First Launch screen toggle (disabled by default)
- **Metrics**: Only non-identifiable, content-free metrics (error codes, TTFT, tokens/sec, memory peaks)
- **Export Format**: Human-friendly interpretations alongside raw data (e.g., "TTFT: 2.8s (Okay)")

## Error Messages (PRD Section 7)

| Condition | User Message |
|-----------|--------------|
| Input text is too long | "Please select a smaller amount of text for this version." |
| Engine fails to generate | "An error occurred. Please try again." |
| Device is out of memory | "Not enough memory to process this text." |
| User cancels/dismisses | (UI simply dismisses, no message needed) |

## Agent OS Directory Structure

The `.agent-os/` directory contains agent-related documentation and operational files:

```
.agent-os/
├── docs/                    # Workflow and process documentation
│   └── github-workflow.md   # Feature branch workflow implementation
├── product/                 # Product management files
│   └── roadmap.md          # Product roadmap and feature planning
├── scripts/                 # Helper scripts for agents (to be created)
└── agent-instructions.md    # Core agent behavioral guidelines (to be created)
```

All agents should:
- Check `.agent-os/` for relevant documentation before starting work
- Save agent-specific documentation and workflows in `.agent-os/docs/`
- Reference `.agent-os/product/` for product planning and roadmap items
- Use `.agent-os/scripts/` for any automation helper scripts