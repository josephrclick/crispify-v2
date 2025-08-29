package com.clickapps.crispify.engine

/**
 * Callback interface for token streaming from native code.
 * Implemented as a SAM (Single Abstract Method) interface for JNI compatibility.
 */
fun interface TokenCallback {
    /**
     * Called when a new token is generated
     * @param token The generated token string
     * @param isFinished true if this is the last token
     */
    fun onToken(token: String, isFinished: Boolean)
}

/**
 * Interface for native llama.cpp library operations
 * This defines the JNI contract for the native implementation
 */
interface LlamaNativeLibrary {
    
    /**
     * Load the GGUF model from assets
     * @param modelPath Path to the model file in assets
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @return true if model loaded successfully
     */
    fun loadModel(modelPath: String, progressCallback: (Float) -> Unit): Boolean
    
    /**
     * Process text through the loaded model with token streaming
     * @param inputText Text to simplify
     * @param tokenCallback Callback for token-by-token streaming
     */
    fun processText(inputText: String, tokenCallback: TokenCallback)
    
    /**
     * Cancel the current text processing operation if running
     */
    fun cancelProcessing()
    
    /**
     * Release the loaded model and free resources
     */
    fun releaseModel()
    
    /**
     * Check if model is currently loaded
     */
    fun isModelLoaded(): Boolean
    
    /**
     * Get current memory usage in bytes
     */
    fun getMemoryUsage(): Long
}

/**
 * JNI implementation that calls native methods
 * The actual native methods will be implemented in C++ using llama.cpp
 */
class LlamaNativeLibraryImpl : LlamaNativeLibrary {
    
    companion object {
        init {
            try {
                // Load the native library when available
                // System.loadLibrary("crispify_llama")
                // For now, we'll use mock implementation
            } catch (e: UnsatisfiedLinkError) {
                // Native library not yet available
            }
        }
    }
    
    // These will be actual JNI native methods when C++ implementation is ready
    // For now, they're stubs that use the mock implementation
    
    external override fun loadModel(modelPath: String, progressCallback: (Float) -> Unit): Boolean
    external override fun processText(inputText: String, tokenCallback: TokenCallback)
    external override fun cancelProcessing()
    external override fun releaseModel()
    external override fun isModelLoaded(): Boolean
    external override fun getMemoryUsage(): Long
}

/**
 * Mock implementation for development and testing
 */
class MockLlamaNativeLibrary : LlamaNativeLibrary {
    
    private var isLoaded = false
    private val mockDelay = 300L // milliseconds per progress step
    @Volatile private var isCancelled = false
    
    override fun loadModel(modelPath: String, progressCallback: (Float) -> Unit): Boolean {
        // Simulate model loading with progress
        for (i in 1..10) {
            Thread.sleep(mockDelay)
            progressCallback(i / 10f)
        }
        isLoaded = true
        return true
    }
    
    override fun processText(inputText: String, tokenCallback: TokenCallback) {
        if (!isLoaded) {
            throw IllegalStateException("Model not loaded")
        }
        
        isCancelled = false
        
        // Simulate text processing with token streaming
        val simplifiedText = inputText
            .replace("utilize", "use")
            .replace("implement", "make")
            .replace("functionality", "feature")
        
        val tokens = simplifiedText.split(Regex("\\s+"))
        
        // Stream tokens with realistic delays
        for ((index, token) in tokens.withIndex()) {
            if (isCancelled) {
                tokenCallback.onToken("", true)
                return
            }
            
            // Simulate token generation delay (shorter for mock)
            Thread.sleep(20)
            
            val isLast = (index == tokens.size - 1)
            tokenCallback.onToken(if (index > 0) " $token" else token, isLast)
        }
    }
    
    override fun cancelProcessing() {
        isCancelled = true
    }
    
    override fun releaseModel() {
        isLoaded = false
    }
    
    override fun isModelLoaded(): Boolean = isLoaded
    
    override fun getMemoryUsage(): Long {
        // Return mock memory usage in bytes (100MB)
        return if (isLoaded) 100 * 1024 * 1024 else 0
    }
}