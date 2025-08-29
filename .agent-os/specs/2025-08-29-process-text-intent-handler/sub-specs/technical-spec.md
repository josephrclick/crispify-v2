# Technical Specification

This is the technical specification for the spec detailed in @.agent-os/specs/2025-08-29-process-text-intent-handler/spec.md

## Technical Requirements

- Manifest & Intent Filter
  - Add intent filter for `android.intent.action.PROCESS_TEXT` with `<data android:mimeType="text/plain"/>`.
  - Ensure the activity is configured to be discoverable by the Process Text system surface (`android:exported="true"`).
  - Provide a clear `android:label` so users see “Crispify” in the chooser.

- Intent Handling
  - Read `Intent.EXTRA_PROCESS_TEXT` (String) and `Intent.EXTRA_PROCESS_TEXT_READONLY` (Boolean; default true).
  - If no text present or text is empty, show a friendly message and allow dismissal.
  - Enforce input length (~1200 token-equivalent) using JTokkit (tiktoken‑compatible) before invoking the engine. If exceeded, show exactly: "Please select a smaller amount of text for this version." (do not call the engine).

- UI (Compose Bottom Sheet)
  - Modal bottom sheet containing: title, progress state, result text area, Copy button, and Close.
  - Visual states: processing, success, error (static messages per PRD §7).
  - Copy action writes processed text to clipboard and shows a snackbar confirmation.

- Processing Pipeline (Local)
  - Route text into the local engine abstraction; initial implementation may use a stub/mock to satisfy UX while native integration is pending.
  - No network permissions; confirm `INTERNET` is not requested in the manifest.

- Diagnostics (Opt-in)
  - If user opted in, record non-identifying metrics locally (e.g., TTFT, tokens/sec; failures) respecting privacy constraints.

- Testing
  - Unit: validate ViewModel state transitions for processing, success, and error.
  - Instrumented: validate intent receipt path on Android 12+ and bottom sheet display; smoke test copy action.
  - Boundary: enforce JTokkit gating at ~1200 tokens (≤1200 proceeds, >1200 blocks with exact PRD message). Ensure engine is not invoked when blocked.

## Token Counting & Limits (JTokkit)

- Library: Use JTokkit (tiktoken‑compatible) to count input tokens deterministically prior to processing.
- Encoding: Use `CL100K_BASE` via the default registry unless project policy selects a different encoding. Keep this centralized for easy changes.
- Limit: Reject input strictly above ~1200 tokens (PRD §5). Do not attempt processing; surface the exact PRD message and allow dismissal.
- Example (Kotlin):
  - `val registry = Encodings.newDefaultEncodingRegistry()`
  - `val enc = registry.getEncoding(EncodingType.CL100K_BASE)`
  - `val tokenCount = enc.encode(input).size`
- Tests: Add boundary tests for 1190 / 1200 / 1201 tokens to ensure correct gating and messaging.

## Prompt Template Integration

- Source: PRD Appendix A (version‑pinned). Use the exact System Preface and Leveling Prompt.
- Storage: Place the template in `res/raw/prompt_template_v1.txt` and expose a tiny `PromptTemplates` helper (in `engine/`) to load and return the strings.
- Composition: Build messages with the System Preface and the Leveling Prompt (insert `{{INPUT}}` with the user text). Do not append runtime instructions to preserve version pinning.

### Artifacts to create
- `app/src/main/res/raw/prompt_template_v1.txt` (exact PRD content)
- `app/src/main/java/com/clickapps/crispify/engine/PromptTemplates.kt` (helper to read raw resource and expose system preface + leveling prompt)
- Unit tests for token boundary and prompt loading

## External Dependencies (Conditional)

- JTokkit (tiktoken‑compatible tokenizer)
  - Gradle: `implementation("com.knuddels:jtokkit:<latest>")`
  - Purpose: Count tokens for input limit enforcement; pick `CL100K_BASE` encoding by default.

## Wiring & Implementation Notes

- ViewModel: Inject a tokenizer service (wrapping JTokkit) and the `PromptTemplates` helper. Perform token gating before calling the engine. On over‑limit, emit the exact PRD error string and skip engine invocation.
- Engine call: Use the helper’s system preface and leveling prompt; insert input into `{{INPUT}}`. Maintain version pinning via the raw file.
- Build: Add the JTokkit dependency and any necessary Proguard/R8 keep rules if minified (none typically required for JTokkit, verify on release build).
