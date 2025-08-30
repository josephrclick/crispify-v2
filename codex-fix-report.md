Codex Fix Report — Model Inference Stability

Branch: codex-fix
Date: 2025-08-30

Overview
- Addressed native crash during tokenization/decoding by correcting batch sizing.
- Removed double prompt construction to align with PRD template handling in Kotlin.
- Aligned user input token limit to PRD (1000 tokens) at the Kotlin pre-check.
- Eliminated duplicate final streaming on completion marker.

What I changed
- Native batch capacity: size to prompt token count to avoid overflow.
  - app/src/main/cpp/llama_wrapper.cpp: initialize batch with `max(128, n_prompt_tokens + 1)`.

- Prompt unification: treat Kotlin-provided text as the final prompt.
  - app/src/main/cpp/llama_wrapper.cpp:
    - Removed native `buildPrompt()` usage; now uses `input_text` as the full prompt.
    - Replaced input-only token check with total prompt token check.
    - Added safety check against context size (`llama_n_ctx`).

- Token limit alignment (PRD):
  - app/src/main/java/com/clickapps/crispify/engine/TokenCounter.kt:
    - `LIMIT_TOKENS` set from 1200 → 1000.

- Streaming cleanup:
  - app/src/main/cpp/llama_wrapper.cpp:
    - When `"### End"` is detected, stop without re-sending the aggregated text (tokens are already streamed and UI strips the marker).

Build & Validation
- Build: `./gradlew assembleDebug` — SUCCESS.
- Target ABI: `arm64-v8a` CMake build completed; 2 warnings in llama.cpp common headers (benign).
- Expected runtime behavior:
  - No SIGABRT on prompt >128 tokens.
  - Real-time token streaming continues until EOS or `"### End"`.
  - No duplicate final chunk when completion marker appears.

How to Test
- Install: `./gradlew installDebug` (device/emulator on API 31+).
- In any app, select complex text → “Crispify”.
- Observe:
  - Streaming output without crash.
  - “### End” not visible in final text (UI strips it).
  - Large selections (>1000 input tokens) rejected early by UI pre-check.

Notes & Follow‑ups
- Tokenizers differ (JTokkit vs llama.cpp); Kotlin pre-check is a guardrail. Native enforces total ≤1200 and ≤context size. If false positives occur near limit, consider relaxing native limit slightly (e.g., 1250) while keeping context bound.
- Error propagation: the native layer logs specific failures; UI currently shows generic errors. A future improvement is to pass structured error codes through JNI without changing the TokenCallback signature (e.g., a parallel lightweight status callback or a terminal token convention parsed by the ViewModel).
- Performance: current settings (n_ctx=2048, n_batch=128, 4 threads) match the spec’s mobile recommendations.

Files Changed
- app/src/main/cpp/llama_wrapper.cpp
- app/src/main/java/com/clickapps/crispify/engine/TokenCounter.kt

Summary
These changes fix the immediate crash, remove prompt duplication, enforce PRD token limits where they matter, and stop redundant final streaming on completion. The app should now run end‑to‑end for on‑device inference and token streaming.

