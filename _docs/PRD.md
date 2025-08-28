# **Crispify — PRD (v1.0 Core Concept Validation)**

## **1\. The Core Idea**

Crispify is an Android utility that makes dense text easy to understand. It's a private, on-device tool that proves a small language model can be genuinely useful for simplifying text within a user's daily workflow.

The goal of v1.0 is to validate this core concept in the most elegant and focused way possible.

## **2\. Goals & Philosophy (v1.0)**

### **Goals**

* **Prove the Concept:** The single most important goal is to demonstrate that on-device text simplification is a viable, useful, and performant concept on modern Android hardware.  
* **Deliver a Single, Magical Interaction:** We will build one feature—text leveling—and deliver it through the most seamless user experience possible: the ACTION\_PROCESS\_TEXT toolbar.  
* **Establish a Stable Foundation:** Build a reliable JNI bridge to the native LLM engine and a solid model delivery pipeline using Play Asset Delivery. This is the bedrock for all future work.  
* **Guarantee Absolute Privacy:** The app will ship with **no** android.permission.INTERNET. This is a non-negotiable feature, not a preference.

### **Non-Goals (What We Are Intentionally NOT Building in v1.0)**

* **Multiple Features:** We are only building **Leveling**. Summary and Jargon modes are deferred.  
* **Multiple Entry Points:** The Share Sheet and clipboard processing are deferred. We will perfect one entry point first.  
* **Complex Input Handling:** There will be no "Smart Split" or text chunking. The app will process a reasonable amount of text and gracefully decline anything larger.  
* **State & Settings:** The app will not remember the user's last choice or have a settings screen. Every run is a clean slate.  
* **"Replace" Functionality:** To keep the interaction simple, v1.0 will only allow the user to **Copy** the generated text, not replace the original selection.

## **3\. The v1.0 Release Plan: Simple, Lovable, Complete**

The entire focus of v1.0 is to deliver a single, polished experience that feels like magic.

### **v1.0 (Core Concept Validation)**

* **Entry Point:** ACTION\_PROCESS\_TEXT only. The user selects text and taps "Crispify" in the floating toolbar.  
* **Core Feature:** **Leveling Mode** only. Rewrites text to be simpler.  
* **User Interface:** A clean, non-intrusive bottom sheet UI built with Jetpack Compose.  
* **Output:** The simplified text streams into the UI token-by-token. The only action is **Copy**.  
* **Onboarding:** A simple, static "First Launch" screen that explains how to use the app and handles initial model loading.

### **Future Versions (Post-Validation)**

Once the core concept is proven, we can confidently build upon the foundation:

* Add Summary and Jargon modes.  
* Introduce the Share Sheet entry point for longer text.  
* Implement "Replace" functionality with an undo option.  
* Build performance optimizations and a settings screen.

## **4\. User Flows**

### **The "First Launch" Onboarding Flow**

1. The user opens Crispify from their home screen for the first time.  
2. A static, full-screen UI appears with a simple message:  
   * A friendly welcome and a brief explanation: "Crispify simplifies text. To get started, select text in any app and find 'Crispify' in the menu."  
   * A subtle loading indicator at the bottom shows progress as the on-device model is prepared for its first use (e.g., "Preparing engine..."). This provides visual feedback and occupies the user during the initial warm-up.  
   * An opt-in toggle for "Local Diagnostics."

### **The Core "Process Text" Flow (v1.0)**

1. In any app, the user selects a paragraph of text.  
2. In the text selection toolbar that appears, they tap **Crispify**.  
3. A bottom sheet slides up from the bottom of the screen, preserving the context of the app behind it.  
4. The sheet immediately begins processing and shows a loading state.  
5. The simplified text streams into the main view.  
6. A single, clear **Copy** button is available for the user to take the result.

## **5\. Functional Requirements (v1.0)**

* **Platform:** Android, Min SDK 31 (Android 12). arm64-v8a architecture only.  
* **Engine:**  
  * Must use a native llama.cpp (or equivalent) library via a JNI wrapper.  
  * The model must be delivered via **Play Asset Delivery** as an install-time asset pack.  
* **Mode: Leveling**  
  * The app will use a single, hardcoded prompt template to rewrite text to a 7th-9th grade reading level. (See Appendix).  
* **Input Handling (Simplified):**  
  * The app will use a tokenizer to perform a quick pre-flight check on the selected text.  
  * If the input exceeds a hardcoded token limit (e.g., \~1200 tokens), the process will stop. The UI will display a simple, static error message: "Please select a smaller amount of text for this version." **There will be no automatic splitting or chunking.**  
* **UI & Rendering:**  
  * The UI must be built entirely with Jetpack Compose and Material 3\.  
  * It must render the model's output token-by-token for a responsive, "live" feel.

## **6\. Privacy & Diagnostics (Simplified)**

* **Privacy:** The AndroidManifest.xml **must not** include android.permission.INTERNET. No user content is ever logged or transmitted.  
* **Local Diagnostics:**  
  * This feature is **opt-in** via a toggle on the "First Launch" screen. It is disabled by default.  
  * If enabled, the app will log only non-identifiable, content-free metrics: error codes, timings (TTFT, tokens/sec), and memory peaks.  
  * A user can export these logs. The exported text file will include a human-friendly interpretation alongside the raw data.  
    * **Example:** TTFT: 2.8s (Okay)  
    * **Example:** Tokens/sec: 15 (Slow)

## **7\. Error Handling (Simplified for v1.0)**

The app will handle errors gracefully with clear, simple messages.

| Condition | User Message |
| :---- | :---- |
| Input text is too long | "Please select a smaller amount of text for this version." |
| Engine fails to generate | "An error occurred. Please try again." |
| Device is out of memory | "Not enough memory to process this text." |
| User cancels/dismisses | (The UI simply dismisses, no message needed) |

## **Appendix A: The v1.0 Prompt Template**

A single, version-pinned prompt will be used for the Leveling feature.

System Preface (internal):  
You are an expert editor who simplifies complex text. You follow instructions precisely. Your output must be clear, factual, and easy to read. You will end your response with a single line that says: \#\#\# End  
**Leveling Prompt:**

\#\#\# Simplified Text

Rewrite the following text in clear, plain language suitable for a 7th-grade reading level. Preserve all key facts, names, and numbers. Use shorter sentences and simple words. Do not add any new information or opinions.

Original Text:  
{{INPUT}}  