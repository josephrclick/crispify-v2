<!--
Thank you for the PR! Please fill out all sections below.

Notes:
- Do not include issue-closing lines (e.g., "Resolves #123").
- Reference the spec folder for this work and include screenshots/GIFs for UI.
- Keep the description concise, outcome-focused, and aligned to the PRD.
-->

## Summary
Brief, outcome-focused summary of the change. Include the spec reference, e.g. `.agent-os/specs/YYYY-MM-DD-feature-name/`.

## Changes

<!-- Use subsections as needed; keep items concise. Check off what applies. -->

### Manifest & Intent Filters
- [ ] Added/updated intent filter(s)
- [ ] Verified exported settings and labels

### Token Counting & Limits
- [ ] Added/updated tokenizer component
- [ ] Enforced input token limit with exact PRD message

### Prompt Template & Engine Integration
- [ ] Added/updated prompt resource(s)
- [ ] Wired template loading and engine usage

### Intent Parsing & UI/UX
- [ ] Parsed intent extras and validated read-only flow
- [ ] Implemented/updated bottom sheet UI and copy action
- [ ] Added/updated error states per PRD

### Diagnostics & Telemetry (opt-in)
- [ ] Recorded non-identifying metrics (no user content)
- [ ] Added export or verification steps as needed

### Build & Config
- [ ] Gradle/config updates (list any)
- [ ] Lint configuration/cleanup (if applicable)

## Testing
- [ ] Ran `./gradlew lint`
- [ ] Ran unit tests: `./gradlew test`
- [ ] Ran instrumented tests (if device available): `./gradlew connectedAndroidTest`
- [ ] Validated ACTION_PROCESS_TEXT chooser from a third-party app
- [ ] Verified read-only handling (copy-only)
- [ ] Verified token boundaries (≈1190 / 1200 / 1201) and PRD message
- [ ] Attached screenshots/GIFs for loading, result, and error states

## Benefits
- 🎯 What user or developer value does this bring?
- 🔒 Privacy/Security improvement or guarantee (if applicable)
- 🐛 Reliability or maintainability gain
- 📊 Better diagnostics or observability (if applicable)

## Notes
- Backwards compatibility:
- Risk/rollback plan:
- Security/Privacy: confirm no `INTERNET` permission and no data egress
- Spec reference: `.agent-os/specs/<folder>/spec.md` (and link to tasks.md)

