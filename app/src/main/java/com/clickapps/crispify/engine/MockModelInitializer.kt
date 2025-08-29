package com.clickapps.crispify.engine

import android.content.Context
import com.clickapps.crispify.ui.onboarding.ModelInitializer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock implementation of ModelInitializer for development and testing
 * Simulates model loading with progress updates
 * 
 * This will be replaced with actual JNI implementation in Task 3
 */
class MockModelInitializer : ModelInitializer {
    
    override fun initialize(onProgress: (Float) -> Unit): Flow<Float> = flow {
        // Simulate model initialization with progress updates
        val steps = 10
        for (i in 1..steps) {
            val progress = i.toFloat() / steps
            onProgress(progress)
            emit(progress)
            
            // Simulate work being done
            delay(300) // 300ms per step = 3 seconds total
        }
    }
}

/**
 * Factory function to create ModelInitializer
 * Returns real LlamaEngine if native library is loaded, mock otherwise
 */
fun createModelInitializer(context: Context): ModelInitializer {
    return if (LlamaNativeLibraryImpl.isNativeLibraryLoaded()) {
        // Use real LlamaEngine with JNI implementation
        LlamaEngine(context)
    } else {
        // Fall back to mock for development/testing
        MockModelInitializer()
    }
}