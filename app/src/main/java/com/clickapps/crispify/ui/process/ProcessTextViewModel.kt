package com.clickapps.crispify.ui.process

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.diagnostics.DiagnosticsManager
import com.clickapps.crispify.diagnostics.ErrorCode
import com.clickapps.crispify.diagnostics.MetricType
import com.clickapps.crispify.engine.LlamaEngine
import com.clickapps.crispify.engine.TokenCounter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for ProcessTextActivity
 * Handles text processing and state management
 */
class ProcessTextViewModel(
    private val llamaEngine: LlamaEngine,
    private val tokenCounter: TokenCounter,
    private val levelingTemplate: String,
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
                
                // Check token length limit (~1200 tokens per PRD)
                val tokens = tokenCounter.count(inputText)
                if (tokens > TokenCounter.LIMIT_TOKENS) {
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
                val prompt = levelingTemplate.replace("{{INPUT}}", inputText)
                val simplifiedText = llamaEngine.processText(prompt)
                
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
    private val tokenCounter: TokenCounter,
    private val levelingTemplate: String,
    private val preferencesManager: PreferencesManager,
    private val diagnosticsManager: DiagnosticsManager? = null
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProcessTextViewModel::class.java)) {
            return ProcessTextViewModel(
                llamaEngine,
                tokenCounter,
                levelingTemplate,
                preferencesManager,
                diagnosticsManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
