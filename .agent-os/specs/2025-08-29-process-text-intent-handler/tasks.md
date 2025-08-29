# Spec Tasks

These are the tasks to be completed for the spec detailed in @.agent-os/specs/2025-08-29-process-text-intent-handler/spec.md

> Created: 2025-08-29
> Status: Ready for Implementation

## Tasks

- [ ] 1. Implement ACTION_PROCESS_TEXT Intent Filter and Manifest Configuration
  - [ ] 1.1 Write tests for manifest configuration validation
  - [ ] 1.2 Add ACTION_PROCESS_TEXT intent filter to AndroidManifest.xml
  - [ ] 1.3 Configure ProcessTextActivity with proper exports and labels
  - [ ] 1.4 Verify intent filter discoverable in Process Text chooser
  - [ ] 1.5 Verify all tests pass

- [ ] 2. Implement Token Counting and Input Validation
  - [ ] 2.1 Write tests for token counting with boundary conditions (1190/1200/1201 tokens)
  - [ ] 2.2 Add JTokkit dependency to build.gradle
  - [ ] 2.3 Create TokenCounter service with CL100K_BASE encoding
  - [ ] 2.4 Implement input length validation in ProcessTextViewModel
  - [ ] 2.5 Add exact PRD error message for over-limit inputs
  - [ ] 2.6 Verify all tests pass

- [ ] 3. Create Prompt Template Resources and Helper
  - [ ] 3.1 Write tests for PromptTemplates helper
  - [ ] 3.2 Create prompt_template_v1.txt in res/raw with exact PRD content
  - [ ] 3.3 Implement PromptTemplates.kt helper in engine package
  - [ ] 3.4 Wire prompt template loading to engine integration
  - [ ] 3.5 Verify all tests pass

- [ ] 4. Implement Intent Parsing and Bottom Sheet UI
  - [ ] 4.1 Write tests for intent parsing and state management
  - [ ] 4.2 Update ProcessTextActivity to parse EXTRA_PROCESS_TEXT and EXTRA_PROCESS_TEXT_READONLY
  - [ ] 4.3 Enhance bottom sheet UI with processing states
  - [ ] 4.4 Implement Copy to clipboard functionality
  - [ ] 4.5 Add proper error state handling with PRD messages
  - [ ] 4.6 Verify all tests pass

- [ ] 5. Integration Testing and Validation
  - [ ] 5.1 Write instrumented tests for end-to-end flow
  - [ ] 5.2 Test ACTION_PROCESS_TEXT from third-party apps
  - [ ] 5.3 Validate read-only context handling
  - [ ] 5.4 Test token limit enforcement and error messages
  - [ ] 5.5 Verify telemetry recording (if diagnostics enabled)
  - [ ] 5.6 Verify all tests pass