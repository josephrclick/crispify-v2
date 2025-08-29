# Spec Requirements Document

> Spec: Process-Text Intent Handler
> Created: 2025-08-29

## Overview

Implement the Android ACTION_PROCESS_TEXT entry point so users can select text in any app and invoke Crispify to simplify it on-device, launching a minimal bottom-sheet flow that processes input and offers copy-to-clipboard output with clear error handling.

## User Stories

### Process selected text via toolbar

As an Android user, I want to choose Crispify from the system Process Text toolbar after selecting text in any app, so that the text is simplified quickly and privately on my device.

Detailed workflow:
- User selects text in a third-party app and taps the Process Text action.
- Chooses “Crispify” from the chooser.
- Crispify opens a bottom sheet overlay, processes the text, and displays simplified output.
- User taps Copy to put the simplified text on the clipboard.

### Respect read-only context

As a user, I want Crispify to respect read-only contexts (EXTRA_PROCESS_TEXT_READONLY) so that it never attempts to modify source text and consistently provides a copy-only affordance.

Detailed workflow:
- If the invoking app marks the request as read-only, Crispify shows processing + copy-only results with no replace or write-back option.

## Spec Scope

1. ACTION_PROCESS_TEXT integration – Manifest intent filter for `android.intent.action.PROCESS_TEXT` with `text/plain`; ensure correct exported settings for system surface.
2. Intent parsing – Read `Intent.EXTRA_PROCESS_TEXT` and `Intent.EXTRA_PROCESS_TEXT_READONLY`; handle empty input and non-text gracefully.
3. UI shell – Launch a minimal Compose bottom sheet to display progress, result, and a single Copy action; Close/Dismiss supported.
4. On-device execution – Route text to the local processing pipeline (stub or engine), conforming to privacy constraints (no network).
5. Error states – Present simple messages per PRD (e.g., too long input, generation failure, OOM), and always allow dismissal. For input too long, show exactly: "Please select a smaller amount of text for this version."
6. Limits – Enforce input size aligned with PRD (~1200 tokens equivalent) using a tokenizer (see Token Counting & Limits) with the above error when exceeded.
7. Telemetry (opt-in) – If diagnostics are enabled, record non-identifying timing and status metrics locally only.
8. Prompt packaging – Store the version‑pinned prompt template as a resource and load via a small helper (see Prompt Template Integration). Wire processing code to use this source of truth.

## Token Counting & Limits (Implementation)

- Tokenizer: Use a tiktoken‑compatible tokenizer to count input tokens prior to processing. On Android/Kotlin, adopt a compatible library (e.g., jtokkit) or ship a minimal tokenizer that matches the PRD’s prompt/model expectations.
- Limit: Block requests exceeding ~1200 tokens (spec limit from PRD §5). This limit applies to the user’s input only (not including system or prompt template overhead).
- UX: If limit exceeded, do not call the engine. Display the exact message per PRD: "Please select a smaller amount of text for this version." and allow dismiss.
- Testing: Add unit tests around boundary conditions (e.g., 1190, 1200, 1201 token inputs) to ensure correct behavior and messaging.

## Prompt Template Integration

- Source of truth: PRD Appendix A (version‑pinned). Use the exact system preface and leveling prompt specified below.
- Storage: Store the prompt template as a versioned, read‑only resource (e.g., `res/raw/prompt_template_v1.txt`) to make updates auditable and avoid localization tooling. Expose via a small `PromptTemplates` helper in `engine/` that loads the raw resource and returns the strings.
- System preface (internal):
  You are an expert editor who simplifies complex text. You follow instructions precisely. Your output must be clear, factual, and easy to read. You will end your response with a single line that says: ### End

- Leveling prompt:
  ### Simplified Text

  Rewrite the following text in clear, plain language suitable for a 7th-grade reading level. Preserve all key facts, names, and numbers. Use shorter sentences and simple words. Do not add any new information or opinions.

  Original Text:
  {{INPUT}}

- Usage: The engine builds a message with the system preface and the leveling prompt, inserting the user input into `{{INPUT}}`. No additional instructions should be appended at runtime to preserve version pinning.

## Out of Scope

- Model download/delivery (Play Asset Delivery) and llama.cpp native integration details.
- Token streaming visuals or advanced UI beyond bottom sheet shell.
- In-place replace of original text (copy-only for v1.0).
- Multi-mode processing (summary, jargon modes); only leveling at 7th–9th grade.
- Any network access or cloud delegation.

## Expected Deliverable

1. From a third-party app, selecting text and choosing Crispify opens a bottom sheet that simplifies text and enables “Copy”.
2. ACTION_PROCESS_TEXT and `text/plain` flow validated on device/emulator (Android 12+), including read-only handling.
3. Prompt resource (`res/raw/prompt_template_v1.txt`) exists and is used via an `engine/PromptTemplates` helper.
4. Token gating enforced with JTokkit; inputs > ~1200 tokens display the exact PRD message and are not processed.
5. Unit tests cover token boundaries (≈1190/1200/1201) and verify no engine invocation when over limit.
6. Build updated to include JTokkit dependency and tests pass.
