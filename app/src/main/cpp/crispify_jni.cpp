#include <jni.h>
#include <android/log.h>
#include <string>
#include <atomic>
#include <memory>
#include "llama_wrapper.h"

#define LOG_TAG "CrispifyJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global reference to the VM for callbacks
static JavaVM* g_vm = nullptr;

// Atomic flag for cancellation
static std::atomic<bool> g_cancel_flag{false};

// Model wrapper instance
static std::unique_ptr<LlamaWrapper> g_model_wrapper;

// Cached JNI references for performance
static jclass g_float_class = nullptr;
static jmethodID g_float_constructor = nullptr;

// JNI OnLoad - called when library is loaded
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    g_vm = vm;
    
    // Cache Float class and constructor for performance
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) {
        jclass local_float_class = env->FindClass("java/lang/Float");
        if (local_float_class) {
            g_float_class = (jclass)env->NewGlobalRef(local_float_class);
            if (g_float_class) {
                g_float_constructor = env->GetMethodID(g_float_class, "<init>", "(F)V");
                if (!g_float_constructor) {
                    // Clean up on failure
                    env->DeleteGlobalRef(g_float_class);
                    g_float_class = nullptr;
                }
            }
            env->DeleteLocalRef(local_float_class);
        }
    }
    
    LOGD("JNI_OnLoad: crispify_llama library loaded");
    return JNI_VERSION_1_6;
}

// JNI OnUnload - called when library is unloaded
JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) == JNI_OK) {
        // Clean up global references
        if (g_float_class) {
            env->DeleteGlobalRef(g_float_class);
            g_float_class = nullptr;
        }
    }
    LOGD("JNI_OnUnload: crispify_llama library unloaded");
}

extern "C" {

// Load model from file path
JNIEXPORT jboolean JNICALL
Java_com_clickapps_crispify_engine_LlamaNativeLibraryImpl_loadModel(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring model_path,
    jobject progress_callback) {
    
    const char* path = env->GetStringUTFChars(model_path, nullptr);
    if (!path) {
        LOGE("loadModel: Failed to get model path");
        return JNI_FALSE;
    }
    
    LOGD("loadModel: Loading model from %s", path);
    
    // Create model wrapper if not exists
    if (!g_model_wrapper) {
        g_model_wrapper = std::make_unique<LlamaWrapper>();
    }
    
    // Progress callback lambda
    auto progress_fn = [env, progress_callback](float progress) {
        if (!progress_callback) return;
        
        // Check for cached references
        if (!g_float_class || !g_float_constructor) {
            LOGD("Float class not cached, skipping callback");
            return;
        }
        
        // Kotlin Function1<Float, Unit> needs to be called with boxed Float
        jclass callback_class = env->GetObjectClass(progress_callback);
        if (!callback_class) return;
        
        jmethodID invoke_method = env->GetMethodID(callback_class, "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;");
        
        if (invoke_method) {
            // Box the float as Float object using cached references
            jobject float_obj = env->NewObject(g_float_class, g_float_constructor, progress);
            
            if (float_obj) {
                // Call the Kotlin lambda
                jobject result = env->CallObjectMethod(progress_callback, invoke_method, float_obj);
                
                // Clean up
                env->DeleteLocalRef(float_obj);
                if (result) env->DeleteLocalRef(result);
            }
            
            // Check for exceptions
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }
        
        env->DeleteLocalRef(callback_class);
    };
    
    // Load the model (stub implementation for now)
    bool success = g_model_wrapper->loadModel(path, progress_fn);
    
    env->ReleaseStringUTFChars(model_path, path);
    
    LOGD("loadModel: %s", success ? "Success" : "Failed");
    return success ? JNI_TRUE : JNI_FALSE;
}

// Process text with token streaming
JNIEXPORT void JNICALL
Java_com_clickapps_crispify_engine_LlamaNativeLibraryImpl_processText(
    JNIEnv* env,
    jobject /*thiz*/,
    jstring input_text,
    jobject token_callback) {
    
    if (!g_model_wrapper || !g_model_wrapper->isModelLoaded()) {
        LOGE("processText: Model not loaded");
        // Call callback with error
        if (token_callback) {
            jclass callback_class = env->GetObjectClass(token_callback);
            jmethodID on_token = env->GetMethodID(callback_class, "onToken", 
                                                  "(Ljava/lang/String;Z)V");
            if (on_token) {
                jstring empty = env->NewStringUTF("");
                env->CallVoidMethod(token_callback, on_token, empty, JNI_TRUE);
                env->DeleteLocalRef(empty);
            }
            env->DeleteLocalRef(callback_class);
        }
        return;
    }
    
    const char* text = env->GetStringUTFChars(input_text, nullptr);
    if (!text) {
        LOGE("processText: Failed to get input text");
        return;
    }
    
    // Reset cancel flag
    g_cancel_flag = false;
    
    LOGD("processText: Processing text of length %zu", strlen(text));
    
    // Token callback lambda
    auto token_fn = [env, token_callback](const std::string& token, bool is_finished) {
        if (!token_callback || g_cancel_flag) return;
        
        jclass callback_class = env->GetObjectClass(token_callback);
        jmethodID on_token = env->GetMethodID(callback_class, "onToken", 
                                              "(Ljava/lang/String;Z)V");
        
        if (on_token) {
            jstring j_token = env->NewStringUTF(token.c_str());
            env->CallVoidMethod(token_callback, on_token, j_token, is_finished);
            env->DeleteLocalRef(j_token);
        }
        
        env->DeleteLocalRef(callback_class);
    };
    
    // Process the text (stub implementation)
    g_model_wrapper->processText(text, token_fn, g_cancel_flag);
    
    env->ReleaseStringUTFChars(input_text, text);
    LOGD("processText: Complete");
}

// Cancel processing
JNIEXPORT void JNICALL
Java_com_clickapps_crispify_engine_LlamaNativeLibraryImpl_cancelProcessing(
    JNIEnv* /*env*/,
    jobject /*thiz*/) {
    
    LOGD("cancelProcessing: Setting cancel flag");
    g_cancel_flag = true;
}

// Release model resources
JNIEXPORT void JNICALL
Java_com_clickapps_crispify_engine_LlamaNativeLibraryImpl_releaseModel(
    JNIEnv* /*env*/,
    jobject /*thiz*/) {
    
    LOGD("releaseModel: Releasing model resources");
    if (g_model_wrapper) {
        g_model_wrapper->releaseModel();
    }
}

// Check if model is loaded
JNIEXPORT jboolean JNICALL
Java_com_clickapps_crispify_engine_LlamaNativeLibraryImpl_isModelLoaded(
    JNIEnv* /*env*/,
    jobject /*thiz*/) {
    
    bool loaded = g_model_wrapper && g_model_wrapper->isModelLoaded();
    LOGD("isModelLoaded: %s", loaded ? "true" : "false");
    return loaded ? JNI_TRUE : JNI_FALSE;
}

// Get memory usage in bytes
JNIEXPORT jlong JNICALL
Java_com_clickapps_crispify_engine_LlamaNativeLibraryImpl_getMemoryUsage(
    JNIEnv* /*env*/,
    jobject /*thiz*/) {
    
    jlong usage = 0;
    if (g_model_wrapper) {
        usage = g_model_wrapper->getMemoryUsage();
    }
    LOGD("getMemoryUsage: %lld bytes", (long long)usage);
    return usage;
}

} // extern "C"