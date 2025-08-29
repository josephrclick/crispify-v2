package com.clickapps.crispify.engine

import com.clickapps.crispify.ui.onboarding.ModelInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Custom exception for model initialization failures
 */
class ModelInitializationException(message: String, cause: Throwable? = null) : 
    Exception(message, cause)

/**
 * Main engine for llama.cpp integration
 * Implements ModelInitializer interface for use with FirstLaunchViewModel
 */
class LlamaEngine(
    private val nativeLibrary: LlamaNativeLibrary = createNativeLibrary()
) : ModelInitializer {
    
    @Volatile
    private var initialized = false
    
    /**
     * Initialize the model with progress updates
     * Emits progress values from 0.0 to 1.0
     */
    override fun initialize(onProgress: (Float) -> Unit): Flow<Float> = flow {
        try {
            // Reset state
            initialized = false
            
            // Emit initial progress
            emit(0f)
            onProgress(0f)
            
            // Load model from assets with progress callbacks
            val modelPath = "models/crispify_model.gguf"
            
            val loadSuccess = withContext(Dispatchers.IO) {
                nativeLibrary.loadModel(modelPath) { progress ->
                    // Progress callback from native code
                    onProgress(progress)
                }
            }
            
            if (!loadSuccess) {
                throw ModelInitializationException("Failed to load model from $modelPath")
            }
            
            initialized = true
            
            // Emit final progress
            emit(1.0f)
            onProgress(1.0f)
            
        } catch (e: Exception) {
            // Clean up on failure
            release()
            
            when (e) {
                is ModelInitializationException -> throw e
                else -> throw ModelInitializationException(
                    "Model initialization failed: ${e.message}", e
                )
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Process text through the model with token streaming
     * @param inputText Text to simplify
     * @param onToken Callback for each generated token
     */
    suspend fun processText(inputText: String, onToken: (String, Boolean) -> Unit) {
        if (!initialized) {
            throw IllegalStateException("Model not initialized. Call initialize() first.")
        }
        
        withContext(Dispatchers.IO) {
            try {
                nativeLibrary.processText(inputText) { token, isFinished ->
                    onToken(token, isFinished)
                }
            } catch (e: Exception) {
                throw ModelInitializationException(
                    "Failed to process text: ${e.message}", e
                )
            }
        }
    }
    
    /**
     * Cancel any ongoing text processing
     */
    fun cancelProcessing() {
        nativeLibrary.cancelProcessing()
    }
    
    /**
     * Check if model is initialized
     */
    fun isInitialized(): Boolean = initialized && nativeLibrary.isModelLoaded()
    
    /**
     * Get current memory usage
     */
    fun getMemoryUsage(): Long = nativeLibrary.getMemoryUsage()
    
    /**
     * Release model resources
     */
    fun release() {
        initialized = false
        nativeLibrary.releaseModel()
    }
    
    companion object {
        /**
         * Create the appropriate native library implementation
         * Returns mock for development, real JNI when available
         */
        private fun createNativeLibrary(): LlamaNativeLibrary {
            return if (LlamaNativeLibraryImpl.isNativeLibraryLoaded()) {
                // Use real JNI implementation
                LlamaNativeLibraryImpl()
            } else {
                // Native library not available, use mock
                MockLlamaNativeLibrary()
            }
        }
    }
}

/**
 * Diagnostics data for the model
 */
data class ModelDiagnostics(
    val isInitialized: Boolean,
    val memoryUsageMB: Float,
    val modelPath: String,
    val lastError: String? = null
)