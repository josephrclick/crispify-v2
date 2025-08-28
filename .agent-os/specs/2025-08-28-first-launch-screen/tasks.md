# Spec Tasks

These are the tasks to be completed for the spec detailed in @.agent-os/specs/2025-08-28-first-launch-screen/spec.md

> Created: 2025-08-28
> Status: Ready for Implementation

## Tasks

- [x] 1. Create FirstLaunchScreen UI components
  - [x] 1.1 Write tests for FirstLaunchScreen composable
  - [x] 1.2 Create FirstLaunchScreen.kt with Material 3 components
  - [x] 1.3 Add placeholder branding box and static text content
  - [x] 1.4 Implement LinearProgressIndicator for model loading
  - [x] 1.5 Add diagnostics Switch component with label
  - [x] 1.6 Create dismissal Button (disabled during loading)
  - [x] 1.7 Apply proper theming and layout constraints
  - [x] 1.8 Verify all UI tests pass

- [x] 2. Implement state management and persistence
  - [x] 2.1 Write tests for FirstLaunchViewModel
  - [x] 2.2 Create FirstLaunchViewModel with StateFlow
  - [x] 2.3 Set up DataStore Preferences dependency
  - [x] 2.4 Implement first-launch flag persistence
  - [x] 2.5 Add diagnostics preference persistence
  - [x] 2.6 Handle configuration changes properly
  - [x] 2.7 Verify all state management tests pass

- [x] 3. Integrate model initialization system
  - [x] 3.1 Write tests for model loading callbacks
  - [x] 3.2 Create ModelInitializer interface and mock implementation
  - [x] 3.3 Add JNI wrapper stubs for llama.cpp integration
  - [x] 3.4 Implement progress callback mechanism
  - [x] 3.5 Add error handling for initialization failures
  - [x] 3.6 Connect model loading to UI progress updates
  - [x] 3.7 Verify all integration tests pass

- [x] 4. Configure navigation and app entry point
  - [x] 4.1 Write tests for first-launch detection
  - [x] 4.2 Update MainActivity to check first-launch flag
  - [x] 4.3 Add FirstLaunchScreen to navigation graph
  - [x] 4.4 Implement back button blocking during loading
  - [x] 4.5 Handle successful dismissal navigation
  - [x] 4.6 Test complete first-launch flow
  - [x] 4.7 Verify all navigation tests pass

- [x] 5. Create DiagnosticsManager system
  - [x] 5.1 Write tests for DiagnosticsManager
  - [x] 5.2 Create DiagnosticsManager class
  - [x] 5.3 Implement opt-in/opt-out logic
  - [x] 5.4 Add privacy-preserving metric collection stubs
  - [x] 5.5 Ensure no network calls are made
  - [x] 5.6 Verify all diagnostics tests pass