package com.clickapps.crispify.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Route composable that connects FirstLaunchScreen with its ViewModel
 * This handles the integration between UI and state management
 * 
 * @param onDismiss Callback when the user dismisses the screen
 */
@Composable
fun FirstLaunchRoute(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: FirstLaunchViewModel = viewModel(
        factory = FirstLaunchViewModelFactory(context)
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    FirstLaunchScreen(
        isModelLoading = uiState.isModelLoading,
        modelLoadingProgress = uiState.modelLoadingProgress,
        isDiagnosticsEnabled = uiState.isDiagnosticsEnabled,
        onDiagnosticsToggle = { enabled ->
            viewModel.toggleDiagnostics(enabled)
        },
        onDismiss = {
            viewModel.onDismiss()
            onDismiss()
        }
    )
}