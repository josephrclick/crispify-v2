package com.clickapps.crispify.engine

import android.content.Context
import android.util.Log
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
    private val context: Context,
    private val nativeLibrary: LlamaNativeLibrary = createNativeLibrary()
) : ModelInitializer {
    
    private val modelAssetManager = ModelAssetManager(context)
    
    @Volatile
    private var initialized = false
    
    /**
     * Initialize the model with progress updates
     * Emits progress values from 0.0 to 1.0
     */
    override fun initialize(onProgress: (Float) -> Unit): Flow<Float> = flow {
        Log.d(TAG, "Starting model initialization")
        try {
            // Reset state
            initialized = false
            
            // Emit initial progress
            emit(0f)
            onProgress(0f)
            Log.d(TAG, "Initial progress emitted")
            
            // Extract model from assets if needed (0% to 50% progress)
            Log.d(TAG, "Extracting model from assets...")
            val modelPath = withContext(Dispatchers.IO) {
                modelAssetManager.getModelPath { extractProgress ->
                    val scaledProgress = extractProgress * 0.5f // Scale to 0-50%
                    Log.v(TAG, "Model extraction progress: ${extractProgress * 100}%")
                    onProgress(scaledProgress)
                }
            }
            Log.d(TAG, "Model extracted to: $modelPath")
            
            // Emit 50% after extraction
            emit(0.5f)
            onProgress(0.5f)
            
            // Load model into memory (50% to 100% progress)
            Log.d(TAG, "Loading model into memory...")
            val loadSuccess = withContext(Dispatchers.IO) {
                nativeLibrary.loadModel(modelPath) { loadProgress ->
                    val scaledProgress = 0.5f + (loadProgress * 0.5f) // Scale to 50-100%
                    Log.v(TAG, "Model loading progress: ${loadProgress * 100}%")
                    onProgress(scaledProgress)
                }
            }
            Log.d(TAG, "Model load success: $loadSuccess")
            
            if (!loadSuccess) {
                throw ModelInitializationException("Failed to load model from $modelPath")
            }
            
            initialized = true
            Log.d(TAG, "Model initialization complete")
            
            // Emit final progress
            emit(1.0f)
            onProgress(1.0f)
            
        } catch (e: Exception) {
            Log.e(TAG, "Model initialization failed", e)
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
        Log.d(TAG, "processText called with input length: ${inputText.length}")
        if (!initialized) {
            Log.e(TAG, "Model not initialized!")
            throw IllegalStateException("Model not initialized. Call initialize() first.")
        }
        
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Calling native processText...")
                nativeLibrary.processText(inputText) { token, isFinished ->
                    Log.v(TAG, "Token received: '$token', finished: $isFinished")
                    onToken(token, isFinished)
                }
                Log.d(TAG, "Native processText completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process text", e)
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
        private const val TAG = "LlamaEngine"
        
        /**
         * Create the appropriate native library implementation
         * Returns mock for development, real JNI when available
         */
        private fun createNativeLibrary(): LlamaNativeLibrary {
            return if (LlamaNativeLibraryImpl.isNativeLibraryLoaded()) {
                Log.d(TAG, "Using real JNI implementation")
                // Use real JNI implementation
                LlamaNativeLibraryImpl()
            } else {
                Log.d(TAG, "Using mock implementation (native library not loaded)")
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