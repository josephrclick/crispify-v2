package com.clickapps.crispify.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI state for the FirstLaunchScreen
 */
data class FirstLaunchUiState(
    val isModelLoading: Boolean = true,
    val modelLoadingProgress: Float = 0f,
    val isDiagnosticsEnabled: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Interface for model initialization
 * This will be implemented by the actual JNI wrapper in Task 3
 */
interface ModelInitializer {
    /**
     * Initialize the model and emit progress updates
     * @param onProgress callback for progress updates
     * @return Flow of progress values from 0.0 to 1.0
     */
    fun initialize(onProgress: (Float) -> Unit): Flow<Float>
}

/**
 * ViewModel for FirstLaunchScreen
 * Manages UI state, model initialization, and preferences persistence
 */
class FirstLaunchViewModel(
    private val dataStore: DataStore<Preferences>,
    private val modelInitializer: ModelInitializer,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    companion object {
        val FIRST_LAUNCH_COMPLETE_KEY = booleanPreferencesKey("first_launch_complete")
        val DIAGNOSTICS_ENABLED_KEY = booleanPreferencesKey("diagnostics_enabled")
    }

    private val _uiState = MutableStateFlow(FirstLaunchUiState())
    val uiState: StateFlow<FirstLaunchUiState> = _uiState.asStateFlow()

    init {
        loadDiagnosticsPreference()
        initializeModel()
    }

    /**
     * Load diagnostics preference from DataStore
     */
    private fun loadDiagnosticsPreference() {
        viewModelScope.launch(ioDispatcher) {
            dataStore.data
                .map { preferences ->
                    preferences[DIAGNOSTICS_ENABLED_KEY] ?: false
                }
                .collect { enabled ->
                    _uiState.update { currentState ->
                        currentState.copy(isDiagnosticsEnabled = enabled)
                    }
                }
        }
    }

    /**
     * Initialize the AI model with progress tracking
     */
    private fun initializeModel() {
        viewModelScope.launch(ioDispatcher) {
            try {
                modelInitializer.initialize { progress ->
                    // Progress callback for immediate UI updates
                    _uiState.update { currentState ->
                        currentState.copy(modelLoadingProgress = progress)
                    }
                }.collect { progress ->
                    // Flow collection for final progress updates
                    _uiState.update { currentState ->
                        currentState.copy(
                            modelLoadingProgress = progress,
                            isModelLoading = progress < 1.0f
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isModelLoading = false,
                        hasError = true,
                        errorMessage = "Failed to initialize AI model: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Toggle diagnostics preference and persist to DataStore
     */
    fun toggleDiagnostics(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[DIAGNOSTICS_ENABLED_KEY] = enabled
            }
            _uiState.update { currentState ->
                currentState.copy(isDiagnosticsEnabled = enabled)
            }
        }
    }

    /**
     * Handle dismiss action - set first launch complete flag
     */
    fun onDismiss() {
        viewModelScope.launch(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[FIRST_LAUNCH_COMPLETE_KEY] = true
            }
        }
    }

    /**
     * Retry model initialization after error
     */
    fun retryModelInitialization() {
        _uiState.update { currentState ->
            currentState.copy(
                hasError = false,
                errorMessage = null,
                isModelLoading = true,
                modelLoadingProgress = 0f
            )
        }
        initializeModel()
    }
}