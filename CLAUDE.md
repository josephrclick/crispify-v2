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
│   ├── MainActivity.kt          # Main entry point with first-launch detection
│   ├── ProcessTextActivity.kt  # Handles ACTION_PROCESS_TEXT intent
│   ├── data/                   # Data layer
│   │   └── PreferencesManager.kt  # DataStore preferences management
│   ├── diagnostics/            # Diagnostics system
│   │   └── DiagnosticsManager.kt  # Privacy-preserving metrics collection
│   ├── di/                     # Dependency injection (to be implemented)
│   ├── engine/                 # LLM engine integration
│   │   ├── LlamaEngine.kt      # Main engine with progress callbacks
│   │   ├── LlamaNativeLibrary.kt # JNI wrapper interface
│   │   ├── LlamaNativeLibraryImpl.kt # Real JNI implementation
│   │   ├── ModelAssetManager.kt # GGUF model extraction from assets
│   │   └── MockLlamaNativeLibrary.kt # Mock for fallback
│   ├── ui/
│   │   ├── onboarding/         # First launch experience
│   │   │   ├── FirstLaunchScreen.kt
│   │   │   ├── FirstLaunchViewModel.kt
│   │   │   └── FirstLaunchRoute.kt
│   │   ├── process/            # Text processing UI
│   │   │   └── ProcessTextViewModel.kt
│   │   └── theme/              # Material 3 theme configuration
│   └── src/main/cpp/           # Native C++ code
│       ├── crispify_jni.cpp    # JNI wrapper implementation
│       └── llama_wrapper.cpp   # llama.cpp integration (stub)
└── src/main/res/               # Resources (layouts, strings, drawables)
```

## Kotlin Symbol Search Workaround (preferred)

Until tree-sitter MCP `search_code` returns Kotlin classes and functions, prefer the local helper script for Kotlin symbol lookups. It prints Kotlin definitions (via ripgrep) and precise usages (via `tree-sitter-mcp find-usage`).

- Preferred command:
  - `./.scripts/kotlin-symbols.sh <SymbolName> [directory] [pathPattern] [json]`

- Examples:
  ```bash
  ./.scripts/kotlin-symbols.sh MainActivity
  ./.scripts/kotlin-symbols.sh LlamaEngine
  ./.scripts/kotlin-symbols.sh MainContent
  # Custom path and JSON output for usages:
  ./.scripts/kotlin-symbols.sh MainActivity /home/joe/dev/projects/crispify/v2/main app/src/main/java json
  ```

Always use this script for Kotlin symbol searches instead of `search_code` until the Kotlin index is fixed.

## Implementation Status

### ✅ Completed Features

1. **ProcessTextActivity**: Handles ACTION_PROCESS_TEXT intent with Material 3 bottom sheet (PR #2)
2. **First Launch Screen**: Complete onboarding experience with model initialization and diagnostics opt-in (PR #2)
3. **State Management**: ViewModel + DataStore Preferences for persistence (PR #2)
4. **Diagnostics System**: Privacy-preserving local metrics collection (opt-in) (PR #2)
5. **JNI Integration**: Full JNI wrapper with Kotlin callback support and memory management (PR #9, #10)
6. **Token Streaming**: Real-time token-by-token display from native to UI (PR #9, #10)
7. **Model Asset Manager**: GGUF model extraction and validation from APK assets (PR #9)
8. **llama.cpp Foundation**: Submodule integration and build infrastructure (PR #9)

### 🚧 Pending Implementation

1. **Model Inference**: Complete llama.cpp inference implementation (currently stub)
2. **Model Asset Delivery**: Google Play Asset Delivery for production GGUF model
3. **Production Testing**: Device testing across Android 12+ devices
4. **Remove Debug Logging**: Strip all verbose logging before release for privacy

## Current Development State

- **Active Branch**: `fix/jni-callback-debug-logging` (PR #10)
- **Main Branch**: `main`
- **Last Feature**: LLM Integration - JNI wrapper and llama.cpp foundation (2025-08-29)
- **Implementation Status**: Real JNI implementation active with stub llama.cpp wrapper
- **Debug Logging**: ⚠️ Verbose logging enabled for development only - will be removed before release to honor privacy commitments
- **Next Priority**: Complete llama.cpp model inference implementation

## Testing Strategy

- Unit tests: JUnit in `src/test/`
- Instrumented tests: AndroidJUnit in `src/androidTest/`
- Test runner: `androidx.test.runner.AndroidJUnitRunner`
- **Known Issues**: Some unit tests have mocking issues with DataStore (functionality works)

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
│   ├── github-workflow.md   # Feature branch workflow implementation
│   └── post-imp.md         # Post-implementation completion workflow
├── product/                 # Product management files
│   └── roadmap.md          # Product roadmap and feature planning
├── recaps/                  # Implementation recaps
│   └── 2025-08-28-first-launch-screen.md  # First launch feature recap
├── specs/                   # Feature specifications
│   └── 2025-08-28-first-launch-screen/    # First launch spec and tasks
├── scripts/                 # Helper scripts for agents (to be created)
└── agent-instructions.md    # Core agent behavioral guidelines (to be created)
```

All agents should:
- Check `.agent-os/` for relevant documentation before starting work
- Follow the post-implementation workflow in `.agent-os/docs/post-imp.md`
- Save agent-specific documentation and workflows in `.agent-os/docs/`
- Reference `.agent-os/product/` for product planning and roadmap items
- Use `.agent-os/scripts/` for any automation helper scripts
- Create specs in `.agent-os/specs/` before major feature implementation
- Document completed work in `.agent-os/recaps/`

## Agent OS Workflow

### CRITICAL: Branch Management
All development MUST happen on feature branches. The system will:
1. Create/switch to a feature branch in Step 3 of `/execute-tasks`
2. Name it from the spec folder without the date prefix (e.g., `2025-08-28-first-launch-screen` → `first-launch-screen`)
3. Prevent direct commits to `main` via pre-commit hook

### Understanding Subagents
When Agent OS instructions mention `subagent="X"`, use the Task tool:
- `subagent_type="X"`
- Copy the REQUEST text as the prompt
- See `.agent-os/docs/subagent-usage-guide.md` for examples

### Workflow Phases
`/execute-tasks` runs three mandatory phases:
1. Setup (including branch creation)
2. Task Execution (iterate through all selected parent tasks)
3. Post-Execution (tests, commit, PR) — automatic, no permission needed

### Common Commands
```bash
# Start new feature branch from a spec folder
./.agent-os/scripts/start-feature.sh 2025-08-28-feature-name

# Install git hooks
./.agent-os/scripts/install-hooks.sh

# Check current branch
git branch --show-current
```