package com.clickapps.crispify.ui.process

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.diagnostics.DiagnosticsManager
import com.clickapps.crispify.diagnostics.ErrorCode
import com.clickapps.crispify.diagnostics.MetricType
import com.clickapps.crispify.engine.LlamaEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for ProcessTextActivity
 * Handles text processing and state management
 */
class ProcessTextViewModel(
    private val llamaEngine: LlamaEngine,
    private val preferencesManager: PreferencesManager,
    private val diagnosticsManager: DiagnosticsManager? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProcessTextUiState())
    val uiState: StateFlow<ProcessTextUiState> = _uiState.asStateFlow()
    
    /**
     * Process the selected text through the LLM engine
     */
    fun processText(inputText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            
            // Track processing start time for metrics
            val startTime = System.currentTimeMillis()
            var timeToFirstToken = 0L
            
            try {
                // Check if model is initialized
                if (!llamaEngine.isInitialized()) {
                    // Initialize model if needed
                    llamaEngine.initialize { progress ->
                        // Progress updates could be shown if needed
                    }.collect()
                }
                
                // Check text length limit (~1200 tokens, roughly 4800 characters)
                if (inputText.length > 4800) {
                    diagnosticsManager?.recordError(ErrorCode.TEXT_TOO_LONG)
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = "Please select a smaller amount of text for this version."
                        )
                    }
                    return@launch
                }
                
                // Process the text
                timeToFirstToken = System.currentTimeMillis() - startTime
                val simplifiedText = llamaEngine.processText(formatPrompt(inputText))
                
                // Extract the result (remove the "### End" marker if present)
                val cleanedText = simplifiedText
                    .substringBefore("### End")
                    .trim()
                
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        processedText = cleanedText,
                        error = null
                    )
                }
                
                // Track diagnostics if enabled
                val processingTime = System.currentTimeMillis() - startTime
                val memoryUsedMB = llamaEngine.getMemoryUsage() / (1024 * 1024)
                val tokensPerSecond = if (processingTime > 0) {
                    (cleanedText.split(" ").size * 1000.0) / processingTime
                } else 0.0
                
                diagnosticsManager?.recordProcessingSession(
                    inputLength = inputText.length,
                    outputLength = cleanedText.length,
                    timeToFirstToken = timeToFirstToken,
                    tokensPerSecond = tokensPerSecond,
                    memoryUsedMB = memoryUsedMB
                )
                
            } catch (e: OutOfMemoryError) {
                diagnosticsManager?.recordError(ErrorCode.OUT_OF_MEMORY)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Not enough memory to process this text."
                    )
                }
            } catch (e: Exception) {
                diagnosticsManager?.recordError(ErrorCode.PROCESSING_FAILED)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "An error occurred. Please try again."
                    )
                }
            }
        }
    }
    
    /**
     * Format the input text with the leveling prompt template
     * Based on PRD Appendix A
     */
    private fun formatPrompt(inputText: String): String {
        return """
            ### Simplified Text
            
            Rewrite the following text in clear, plain language suitable for a 7th-grade reading level. Preserve all key facts, names, and numbers. Use shorter sentences and simple words. Do not add any new information or opinions.
            
            Original Text:
            $inputText
        """.trimIndent()
    }
}

/**
 * UI state for ProcessTextActivity
 */
data class ProcessTextUiState(
    val isProcessing: Boolean = false,
    val processedText: String = "",
    val error: String? = null
)

/**
 * Factory for ProcessTextViewModel
 */
class ProcessTextViewModelFactory(
    private val llamaEngine: LlamaEngine,
    private val preferencesManager: PreferencesManager,
    private val diagnosticsManager: DiagnosticsManager? = null
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProcessTextViewModel::class.java)) {
            return ProcessTextViewModel(llamaEngine, preferencesManager, diagnosticsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}