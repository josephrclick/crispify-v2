# **Crispify Tech Stack (v1.0 SLC)**

**Context:** This document outlines the lean and focused technical stack for the v1.0 "Simple, Lovable, Complete" (SLC) release of Crispify. The goal is to validate the core concept with minimal complexity.

## **Core Platform**

This is the fundamental foundation of the application.

* **App Framework:** Android Native  
* **Language:** Kotlin (latest stable version)  
* **Minimum SDK:** 31 (Android 12\)  
* **Target/Compile SDK:** 35 (or latest stable)  
* **Architecture:** arm64-v8a only (simplifies native builds)  
* **JVM Target:** 17

## **UI Framework**

The entire user interface will be modern and declarative.

* **UI Toolkit:** Jetpack Compose (using the latest BOM)  
* **Component Library:** Material 3  
* **Activity Integration:** androidx.activity:activity-compose

## **Architecture & State**

We will keep the architecture simple and state management localized to the UI layer for v1.0.

* **State Management:** Kotlin StateFlow for managing UI state within ViewModels.  
* **Dependency Injection:** Manual DI or Hilt (if needed for scoping the engine).  
* **Core Libraries:** androidx.core:core-ktx, androidx.lifecycle:lifecycle-runtime-ktx

*Note: DataStore is intentionally excluded from v1.0 as we are not persisting any user settings.*

## **LLM Engine & Delivery**

This is the core of the application's functionality.

* **Native Engine:** A JNI wrapper for llama.cpp (or equivalent native library).  
* **Model Format:** GGUF (quantized for mobile performance).  
* **Model Delivery:** **Play Asset Delivery** (install-time asset pack). This is crucial for keeping the initial APK small and ensuring the model is available on first launch.  
* **Processing:** Strictly on-device. The app will have **no** INTERNET permission.

## **Application Features (v1.0 Scope)**

The feature set is intentionally minimal to focus on the core experience.

* **Text Intake:** ACTION\_PROCESS\_TEXT intent only.  
* **UI Mode:** Bottom Sheet (Activity with a Dialog theme).  
* **Processing Mode:** **Leveling** only.

## **Testing & Quality**

A pragmatic approach to ensure stability without excessive overhead.

* **Unit Testing:** JUnit 5 for business logic and ViewModel tests.  
* **Mocking:** MockK for creating test doubles.  
* **Code Quality:** Android Lint (standard rules).

*Note: Static analysis tools like Detekt and extensive UI testing suites are deferred to future versions.*

## **Build & Development**

Configuration for a streamlined development and release process.

* **Build Tool:** Gradle (latest stable via wrapper).  
* **Android Gradle Plugin (AGP):** Latest stable version.  
* **IDE:** Android Studio (latest stable version).  
* **Version Control:** Git.  
* **Repository Structure:** Simple, single-module app structure.  
* **Deployment:** Release builds via standard Android App Bundle (AAB) upload to the Play Store, which will manage the asset pack delivery.