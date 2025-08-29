# Repository Guidelines

## Project Structure & Module Organization
- `app/`: Android application module.
  - `src/main/java/com/clickapps/crispify/…`: Kotlin sources (engine, ui, data, diagnostics).
  - `src/main/res/`: resources; `AndroidManifest.xml` in `src/main/`.
  - Tests in `src/test/` (unit, Robolectric) and `src/androidTest/` (instrumented).
- Gradle files: root `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts`.
- Source of truth: `_docs/PRD.md` (align all changes to the PRD).

## Build, Test, and Development Commands
```bash
./gradlew assembleDebug         # Build debug APK
./gradlew installDebug          # Install on connected device/emulator
./gradlew test                  # Run unit tests (Robolectric-enabled)
./gradlew connectedAndroidTest  # Run instrumented tests
./gradlew lint                  # Android Lint checks
./gradlew clean                 # Clean build outputs
```
Tip: Use the latest stable Android Studio; min SDK 31, target/compile SDK 36.

## Coding Style & Naming Conventions
- Kotlin, Jetpack Compose, Material 3.
- Indentation: 4 spaces; max line length follow IDE defaults.
- Packages: lowercase dotted (`com.clickapps.crispify`).
- Classes/objects: PascalCase; functions/vars: camelCase.
- Compose: suffix screens with `…Screen`, routes with `…Route`, ViewModels with `…ViewModel`.
- Run `./gradlew lint` and fix warnings that affect correctness or UX.

## Testing Guidelines
- Frameworks: JUnit, Robolectric (unit), AndroidX Test/Compose Test (instrumented).
- Naming: mirror class under test and suffix with `Test` (e.g., `FirstLaunchViewModelTest.kt`).
- Scope: prefer small, deterministic tests; mock Android/services as needed.
- Run unit tests locally (`./gradlew test`) and instrumented on a device/AVD (`connectedAndroidTest`).

## Commit & Pull Request Guidelines
- Commits: follow Conventional Commits when possible (e.g., `feat: …`, `fix: …`, `docs(scope): …`).
- PRs: clear description, rationale, and screenshots/GIFs for UI; link issues; include a brief test plan and any risk/rollback notes.
- Keep changes aligned to the PRD; avoid unrelated refactors in feature PRs.

## Security & Configuration Tips
- Do not add the `INTERNET` permission; all processing is on‑device.
- Model delivery and native LLM integration are planned; keep interfaces stable (`engine/` package) and avoid leaking user data in logs.

## Agent OS Workflow (Quickstart)

This repo uses Agent OS to organize specs, tasks, and execution. These rules let any agent (or you) run the same flows consistently.

### Branch Rules (MANDATORY)
- Never commit directly to `main`.
- Create a feature branch derived from the spec folder name with the date removed.
  - Example: `.agent-os/specs/2025-08-28-first-launch-screen/` → branch `first-launch-screen`.
- Install hooks to enforce this: `./.agent-os/scripts/install-hooks.sh` (looks for `.claude/hooks/pre-commit-checks.sh`).

### Common Commands
```bash
# Install git hooks
./.agent-os/scripts/install-hooks.sh

# Start a feature branch from a spec folder (removes date prefix)
./.agent-os/scripts/start-feature.sh 2025-08-28-first-launch-screen

# Build, test (from above sections)
./gradlew test
./gradlew connectedAndroidTest   # if device available
```

### Reference Files
- Source of truth PRD: `_docs/PRD.md`.
- Agent OS instructions: `.agent-os/agent-instructions.md` (phase rules & post‑execution checklist).
- Git workflow doc: `.agent-os/docs/github-workflow.md`.

## Command‑First Agent OS (for agents)

To ensure consistent, tool‑agnostic execution, always enter Agent OS via the command files in `.claude/commands/`. Do not parse or jump directly into `.agent-os/instructions/`, and do not run scripts directly.

- Allowed entrypoints (read first, then follow the referenced instructions):
  - `.claude/commands/analyze-product.md` → `@.agent-os/instructions/core/analyze-product.md`
  - `.claude/commands/plan-product.md` → `@.agent-os/instructions/core/plan-product.md`
  - `.claude/commands/create-spec.md` → `@.agent-os/instructions/core/create-spec.md`
  - `.claude/commands/create-tasks.md` → `@.agent-os/instructions/core/create-tasks.md`
  - `.claude/commands/execute-tasks.md` → `@.agent-os/instructions/core/execute-tasks.md` (Do not stop after Phase 2; proceed to post‑execution.)
  - `.claude/commands/context7.md` → Use Context7 + sequential‑thinking tools before writing or debugging code.

- Prohibited behaviors (for agents):
  - Do not run `.agent-os/scripts/*` directly; follow the branching/hooks steps only through the referenced instruction flow.
  - Do not skip subagent steps (e.g., `context-fetcher`, `date-checker`, `file-creator`).
  - Do not apply patches outside the current task’s scope or bypass the three‑phase `/execute-tasks` flow.

- Precedence and sources of truth:
  - Product scope and constraints: `_docs/PRD.md`.
  - Operational flow and entrypoints: `.claude/commands/*` (authoritative); they delegate to `.agent-os/instructions/*`.
  - If any guidance conflicts, follow the `.claude/commands/*` file and its referenced instruction.

## Kotlin Symbol Search (Workaround — use this first)

Tree-sitter MCP `search_code` may not return Kotlin classes/functions. Prefer the repo script that combines ripgrep defs with tree-sitter usages:

```bash
./.scripts/kotlin-symbols.sh <SymbolName> [directory] [pathPattern] [json]

# Examples
./.scripts/kotlin-symbols.sh MainActivity
./.scripts/kotlin-symbols.sh LlamaEngine
./.scripts/kotlin-symbols.sh MainContent
./.scripts/kotlin-symbols.sh MainActivity /home/joe/dev/projects/crispify/v2/main app/src/main/java json
```

Use this script for Kotlin symbol lookups instead of `search_code` until the Kotlin index is fixed.
