#include <jni.h>
#include <android/log.h>
#include <utility>

#define LOG_TAG "TokenCallback"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Static callback dispatcher for token streaming
// This file contains utility functions for managing JNI callbacks

/**
 * Helper to attach current thread to JVM if needed
 * Returns JNIEnv* and whether we need to detach later
 */
std::pair<JNIEnv*, bool> getJNIEnv(JavaVM* vm) {
    JNIEnv* env = nullptr;
    bool need_detach = false;
    
    int status = vm->GetEnv((void**)&env, JNI_VERSION_1_6);
    if (status == JNI_EDETACHED) {
        // Attach current thread
        if (vm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
            need_detach = true;
        } else {
            LOGD("Failed to attach thread");
            return {nullptr, false};
        }
    }
    
    return {env, need_detach};
}

/**
 * Helper to safely detach thread if needed
 */
void detachThreadIfNeeded(JavaVM* vm, bool need_detach) {
    if (need_detach) {
        vm->DetachCurrentThread();
    }
}