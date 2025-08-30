package com.clickapps.crispify.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Manages model assets extraction and validation
 */
class ModelAssetManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelAssetManager"
        // Asset is placed under app/src/main/assets/models/
        private const val MODEL_ASSET_PATH = "models/gemma-3-270m-it-qat-Q5_K_M.gguf"
        private const val MODEL_FILE_NAME = "crispify_model.gguf"
        private const val MODEL_DIR = "models"
        private const val META_FILE_NAME = "crispify_model.meta"
        
        // Expected model size for validation (approximate)
        private const val MIN_MODEL_SIZE = 100_000_000L // 100MB minimum
    }
    
    /**
     * Get the path to the extracted model file
     * Extracts from assets if not already present
     * @param progressCallback Callback for extraction progress (0.0 to 1.0)
     * @return Absolute path to the model file
     */
    suspend fun getModelPath(progressCallback: (Float) -> Unit = {}): String = 
        withContext(Dispatchers.IO) {
            val modelDir = File(context.filesDir, MODEL_DIR)
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val modelFile = File(modelDir, MODEL_FILE_NAME)
            val metaFile = File(modelDir, META_FILE_NAME)

            // Determine current asset size if available (used to detect model changes)
            val assetSize = try {
                context.assets.openFd(MODEL_ASSET_PATH).use { it.length }
            } catch (e: IOException) { -1L }

            // Check if model already extracted and valid
            if (modelFile.exists() && isModelValid(modelFile)) {
                // Verify meta matches current asset path and (if known) size
                val needsReplace = try {
                    val meta = if (metaFile.exists()) metaFile.readText() else ""
                    val assetLine = meta.lineSequence().firstOrNull { it.startsWith("assetPath:") } ?: ""
                    val sizeLine = meta.lineSequence().firstOrNull { it.startsWith("assetSize:") } ?: ""
                    val metaAsset = assetLine.substringAfter(":", "").trim()
                    val metaSize = sizeLine.substringAfter(":", "").trim().toLongOrNull()

                    val assetMismatch = metaAsset.isNotEmpty() && metaAsset != MODEL_ASSET_PATH
                    val sizeMismatch = assetSize > 0 && (metaSize ?: -1L) > 0 && metaSize != assetSize
                    val fileSizeMismatch = assetSize > 0 && modelFile.length() != assetSize
                    assetMismatch || sizeMismatch || fileSizeMismatch
                } catch (e: Exception) {
                    // If any error reading meta, conservatively replace
                    true
                }

                if (!needsReplace) {
                    Log.d(TAG, "Model already extracted and up-to-date: ${modelFile.absolutePath}")
                    progressCallback(1.0f)
                    return@withContext modelFile.absolutePath
                } else {
                    Log.d(TAG, "Model asset changed, re-extracting new model")
                    // Remove old files
                    runCatching { modelFile.delete() }
                    runCatching { metaFile.delete() }
                }
            }
            
            // Extract model from assets
            Log.d(TAG, "Extracting model from assets to: ${modelFile.absolutePath}")
            extractModelFromAssets(modelFile, metaFile, progressCallback)
            
            // Validate extracted model
            if (!isModelValid(modelFile)) {
                modelFile.delete()
                throw IOException("Extracted model validation failed")
            }
            
            Log.d(TAG, "Model extraction complete: ${modelFile.absolutePath}")
            modelFile.absolutePath
        }
    
    /**
     * Extract model from assets to private storage
     */
    private fun extractModelFromAssets(targetFile: File, metaFile: File, progressCallback: (Float) -> Unit) {
        try {
            context.assets.open(MODEL_ASSET_PATH).use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    
                    // Get asset size for progress calculation
                    val assetSize = try {
                        context.assets.openFd(MODEL_ASSET_PATH).use { it.length }
                    } catch (e: IOException) {
                        // Some assets can't get size, estimate
                        200_000_000L // 200MB estimate
                    }
                    
                    // Copy with progress updates
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        
                        // Calculate and report progress
                        val progress = (totalBytes.toFloat() / assetSize).coerceIn(0f, 1f)
                        progressCallback(progress)
                    }
                    
                    outputStream.flush()
                    progressCallback(1.0f)
                }
            }

            // Write meta file with asset details for future validation
            runCatching {
                val assetSizeReal = try {
                    context.assets.openFd(MODEL_ASSET_PATH).use { it.length }
                } catch (e: IOException) { -1L }
                metaFile.writeText("assetPath: $MODEL_ASSET_PATH\nassetSize: $assetSizeReal\n")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to extract model from assets", e)
            throw IOException("Failed to extract model: ${e.message}", e)
        }
    }
    
    /**
     * Validate that the model file is complete and valid
     */
    private fun isModelValid(modelFile: File): Boolean {
        if (!modelFile.exists()) {
            return false
        }
        
        val fileSize = modelFile.length()
        if (fileSize < MIN_MODEL_SIZE) {
            Log.w(TAG, "Model file too small: $fileSize bytes")
            return false
        }
        
        // Check GGUF magic number (first 4 bytes should be "GGUF")
        try {
            modelFile.inputStream().use { stream ->
                val magic = ByteArray(4)
                if (stream.read(magic) == 4) {
                    val magicString = String(magic, Charsets.UTF_8)
                    if (magicString == "GGUF") {
                        Log.d(TAG, "Model validation passed: GGUF format confirmed")
                        return true
                    } else {
                        Log.e(TAG, "Invalid model format: $magicString")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to validate model", e)
        }
        
        return false
    }
    
    /**
     * Delete the extracted model file (for cleanup or re-extraction)
     */
    fun deleteModel() {
        val modelDir = File(context.filesDir, MODEL_DIR)
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        if (modelFile.exists()) {
            modelFile.delete()
            Log.d(TAG, "Model file deleted")
        }
    }
    
    /**
     * Get the size of the extracted model in bytes
     */
    fun getModelSize(): Long {
        val modelDir = File(context.filesDir, MODEL_DIR)
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        return if (modelFile.exists()) modelFile.length() else 0L
    }
    
    /**
     * Check if model is already extracted
     */
    fun isModelExtracted(): Boolean {
        val modelDir = File(context.filesDir, MODEL_DIR)
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        return modelFile.exists() && isModelValid(modelFile)
    }
}
