package com.clickapps.crispify.engine

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
 * Returns mock for now, will return real implementation later
 */
fun createModelInitializer(): ModelInitializer {
    // TODO: Replace with real JNI implementation when available
    return MockModelInitializer()
}