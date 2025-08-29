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
import com.clickapps.crispify.engine.prompt.PromptTemplates
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking

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
    
    private var currentJob: Job? = null

    /**
     * Process the selected text through the LLM engine.
     * Implements real token streaming as tokens are generated.
     */
    fun processText(inputText: String) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null, processedText = "") }
            
            // Track processing start time for metrics
            val startTime = System.currentTimeMillis()
            var timeToFirstToken = 0L
            var firstTokenReceived = false
            var tokenCount = 0
            
            try {
                // Quick pre-flight token limit check (per PRD)
                val tokens = tokenCounter.count(inputText)
                if (tokens > TokenCounter.LIMIT_TOKENS) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = "Please select a smaller amount of text for this version."
                        )
                    }
                    // Record after updating UI to avoid blocking error surface
                    diagnosticsManager?.recordError(ErrorCode.TEXT_TOO_LONG)
                    return@launch
                }

                // Ensure model is initialized only after passing token check
                if (!llamaEngine.isInitialized()) {
                    llamaEngine.initialize { _ ->
                        // Progress updates could be shown if needed
                    }.collect()
                }
                
                // Build prompt via helper and process the text with real streaming
                val prompt = PromptTemplates.buildFromTemplate(levelingTemplate, inputText)
                val outputBuilder = StringBuilder()
                
                llamaEngine.processText(prompt) { token, isFinished ->
                    runBlocking {
                        if (!firstTokenReceived) {
                            // Capture time to first real token
                            timeToFirstToken = System.currentTimeMillis() - startTime
                            firstTokenReceived = true
                        }
                        
                        if (!isFinished) {
                            // Check for cancellation
                            if (!isActive) {
                                llamaEngine.cancelProcessing()
                                return@runBlocking
                            }
                            
                            // Append token and update UI
                            outputBuilder.append(token)
                            tokenCount++
                            
                            // Extract the result (remove the "### End" marker if present)
                            val cleanedText = outputBuilder.toString()
                                .substringBefore("### End")
                            
                            _uiState.update { 
                                it.copy(processedText = cleanedText, isProcessing = true, error = null) 
                            }
                        } else {
                            // Processing finished
                            val finalText = outputBuilder.toString()
                                .substringBefore("### End")
                                .trim()
                            
                            _uiState.update { 
                                it.copy(processedText = finalText, isProcessing = false, error = null) 
                            }
                            
                            // Track diagnostics if enabled
                            val totalTimeMs = System.currentTimeMillis() - startTime
                            val memoryUsedMB = llamaEngine.getMemoryUsage() / (1024 * 1024)
                            val tokensPerSecond = if (totalTimeMs > 0) {
                                (tokenCount * 1000.0) / totalTimeMs
                            } else 0.0
                            
                            diagnosticsManager?.recordProcessingSession(
                                inputLength = inputText.length,
                                outputLength = finalText.length,
                                timeToFirstToken = timeToFirstToken,
                                tokensPerSecond = tokensPerSecond,
                                memoryUsedMB = memoryUsedMB
                            )
                        }
                    }
                }
                
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
     * Cancel the current text processing operation
     */
    fun cancelProcessing() {
        currentJob?.cancel()
        llamaEngine.cancelProcessing()
        _uiState.update { it.copy(isProcessing = false) }
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
