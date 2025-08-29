⚠️ **Work in Progress (Pre-Alpha)**

This extension is under active development. It’s not production-ready and APIs/UI may change without notice.
If you’re exploring the code, great! If you’re expecting a stable tool, please wait for v1.0.

---

# **Crispify \- An On-Device Text Simplifier for Android**

Crispify is an Android utility that makes dense text easy to understand. It's a private, on-device tool that proves a small language model can be genuinely useful for simplifying text within a user's daily workflow.

## **Project Status: v1.0 (Core Concept Validation)**

This repository contains the source code for the initial "Simple, Lovable, Complete" (SLC) version of Crispify.

**The primary goal is to validate one core concept:** Can we deliver a fast, private, and genuinely useful text simplification experience on a modern Android device? All features are focused exclusively on answering this question.

## **Core Principles**

* **Privacy First:** All text processing happens 100% on-device. The app ships with **no internet permission**, guaranteeing user data never leaves their phone.  
* **Frictionless Experience:** The tool should feel like a native part of the Android OS, integrating seamlessly into existing workflows.  
* **Focused Utility:** Do one thing exceptionally well. For v1.0, that one thing is text leveling.

## **v1.0 Feature Set**

The initial version is intentionally minimal to perfect the core experience.

* Single Feature: Text Leveling  
  The app's only function is to rewrite selected text to a simpler, 7th-grade reading level.  
* Single Entry Point: Process Text  
  Crispify is activated by selecting text in any app and tapping its name in the floating toolbar. It appears as a clean, non-intrusive bottom sheet.  
* Single Action: Copy  
  The user can copy the simplified text to their clipboard with a single tap.

## **Tech Stack**

This project is built with a modern, focused Android tech stack.

* **Language:** Kotlin  
* **UI:** 100% Jetpack Compose with Material 3  
* **Platform:** Android 12 (SDK 31\) and above  
* **Architecture:** arm64-v8a only  
* **LLM Engine:** A native C++ library (like llama.cpp) accessed via a JNI bridge  
* **Model Delivery:** The GGUF model is delivered via Google Play Asset Delivery

## **Getting Started**

This project was initialized from the standard "Empty Activity" template in Android Studio.

1. **Clone the Repository:**  
   git clone \<repository-url\>  
   cd Crispify

2. Open in Android Studio:  
   Open the project in the latest stable version of Android Studio. It should sync and build without any extra configuration.  
3. Run the App:  
   Run the app on a physical device or emulator with API 31+. The initial "Onboarding" screen will appear.

## **Roadmap**

Our development is phased to ensure we build a solid foundation first.

* Phase 1: MVP (Current)  
  Build and validate the core text leveling feature via the Process-Text entry point. Prove the concept is viable and useful.  
* Phase 2: Polish & Performance  
  Refine the user experience with streaming text, animations, and performance optimizations for a wider range of devices.  
* Phase 3: Enhanced Intelligence  
  Expand beyond basic leveling to include features like summarization and jargon explanation, building on the validated foundation.
