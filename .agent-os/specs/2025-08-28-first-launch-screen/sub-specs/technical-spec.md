# Technical Specification

This is the technical specification for the spec detailed in @.agent-os/specs/2025-08-28-first-launch-screen/spec.md

> Created: 2025-08-28
> Version: 1.0.0

## Technical Requirements

### UI Implementation
- Create a full-screen Composable function `FirstLaunchScreen()` using Jetpack Compose
- Implement Material 3 design components with proper theming
- Use a Column layout with centered content alignment
- Add placeholder for app logo/branding (Box with background color until assets available)
- Display static text for app description and usage instructions
- Implement a LinearProgressIndicator for model loading status
- Add a Material 3 Switch component for diagnostics opt-in
- Include a Material 3 Button for dismissal action (enabled after model loads)

### Model Initialization
- Trigger JNI call to initialize llama.cpp engine on screen display
- Load GGUF model from Play Asset Delivery package
- Update progress indicator based on initialization callbacks
- Handle initialization errors gracefully with user-friendly error messages
- Cache initialization state to avoid redundant loading

### State Management
- Use ViewModel to manage screen state and model loading progress
- Implement StateFlow for reactive UI updates
- Persist diagnostics preference using DataStore Preferences
- Store first-launch completion flag using DataStore Preferences
- Handle configuration changes properly to maintain state

### Navigation
- Detect first launch by checking DataStore Preferences flag
- Launch FirstLaunchScreen from MainActivity when flag is not set
- Navigate to main app functionality after dismissal
- Prevent back navigation during model loading
- Set first-launch flag after successful dismissal

### Diagnostics Integration
- Create DiagnosticsManager class for opt-in handling
- Store preference using DataStore Preferences
- Initialize logging only if user opts in
- Respect privacy by collecting only non-identifiable metrics
- No network calls (app has no INTERNET permission)

### Performance Considerations
- Load model initialization asynchronously to prevent UI blocking
- Show progress updates at reasonable intervals (every 10% or 500ms)
- Implement proper lifecycle handling to cancel operations if needed
- Optimize Compose recompositions for smooth progress updates