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
* **LLM Engine:** llama.cpp via JNI bridge with adaptive prompt engineering
* **Model:** Gemma-3 270M IT QAT Q5_K_M (quantized for mobile efficiency)
* **Model Delivery:** Currently bundled in APK assets (Play Asset Delivery planned)

## **Getting Started**

### Prerequisites: Model Installation

**Important:** The app requires a language model to function. Download and place the model file before building:

1. **Download the Model:**  
   Download `gemma-3-270m-it-qat-Q5_K_M.gguf` from:  
   https://huggingface.co/unsloth/gemma-3-270m-it-qat-GGUF
   
2. **Place the Model:**  
   Copy the downloaded file to:  
   `app/src/main/assets/models/gemma-3-270m-it-qat-Q5_K_M.gguf`
   
   Note: The `models` directory may need to be created if it doesn't exist.

### Building the Project

1. **Clone the Repository:**  
   git clone https://github.com/josephrclick/crispify-v2
   cd crispify-v2

2. **Open in Android Studio:**  
   Open the project in the latest stable version of Android Studio. It should sync and build without any extra configuration. 
    
3. **Run the App:**  
   Run the app on a physical device or emulator with API 31+. The initial "Onboarding" screen will appear.

## **Roadmap**

Our development is phased to ensure we build a solid foundation first.

* Phase 1: MVP (Current)  
  Build and validate the core text leveling feature via the Process-Text entry point. Prove the concept is viable and useful.  
* Phase 2: Polish & Performance  
  Refine the user experience with streaming text, animations, and performance optimizations for a wider range of devices.  
* Phase 3: Enhanced Intelligence  
  Expand beyond basic leveling to include features like summarization and jargon explanation, building on the validated foundation.
