# Spec Requirements Document

> Spec: First Launch Screen
> Created: 2025-08-28

## Overview

Implement a First Launch screen that provides initial onboarding when users open Crispify directly from their home screen. This screen explains how to use the app via text selection, handles the initial model preparation, and offers an optional diagnostics opt-in toggle.

## User Stories

### First-Time App Launch

As a new user, I want to understand how to use Crispify when I open it for the first time, so that I know how to access its text simplification features from other apps.

The user opens Crispify from their app launcher and sees a clean, informative screen that explains the app's purpose and usage. The screen displays instructions on selecting text in any app and finding "Crispify" in the text selection menu. While the on-device AI model initializes in the background, a progress indicator shows the preparation status. The user can optionally enable local diagnostics via a toggle switch. Once ready, the user dismisses the screen with a clear action button.

## Spec Scope

1. **Welcome Message** - Display app name placeholder and brief explanation of Crispify's text simplification purpose
2. **Usage Instructions** - Clear, static text explaining how to select text and find Crispify in the selection menu
3. **Model Initialization** - Background loading of the on-device LLM with simple progress indicator
4. **Diagnostics Toggle** - Optional opt-in switch for local, privacy-preserving diagnostics collection
5. **Dismiss Action** - User-controlled dismissal button that becomes active after model preparation

## Out of Scope

- Interactive tutorials or animated demonstrations
- Account creation or sign-in flows
- Network connectivity checks (app has no internet permission)
- Multiple onboarding screens or step-by-step wizards
- Customization options or settings beyond diagnostics toggle

## Expected Deliverable

1. A single, full-screen Compose UI that appears on first direct app launch
2. Model initialization that completes successfully with visual progress feedback
3. Persistent storage of diagnostics preference and first-launch completion state