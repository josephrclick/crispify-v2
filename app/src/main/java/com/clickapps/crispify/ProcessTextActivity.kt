package com.clickapps.crispify

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.diagnostics.DiagnosticsManager
import com.clickapps.crispify.engine.LlamaEngine
import com.clickapps.crispify.ui.process.ProcessTextViewModel
import com.clickapps.crispify.ui.process.ProcessTextViewModelFactory
import com.clickapps.crispify.ui.theme.CrispifyTheme
import kotlinx.coroutines.launch

/**
 * Activity that handles ACTION_PROCESS_TEXT intent
 * This is the main entry point for text simplification
 */
class ProcessTextActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Extract text from the intent
        val selectedText = when {
            intent?.hasExtra(Intent.EXTRA_PROCESS_TEXT) == true -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
            }
            else -> ""
        }
        
        // Check if this is read-only mode
        val isReadOnly = intent?.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true) ?: true
        
        setContent {
            CrispifyTheme {
                ProcessTextScreen(
                    selectedText = selectedText,
                    isReadOnly = isReadOnly,
                    onDismiss = { finish() }
                )
            }
        }
    }
}

/**
 * Main UI for text processing
 * Displays a bottom sheet with the simplified text
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessTextScreen(
    selectedText: String,
    isReadOnly: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = PreferencesManager(context)
    val diagnosticsManager = DiagnosticsManager(preferencesManager.dataStore)
    
    val viewModel: ProcessTextViewModel = viewModel(
        factory = ProcessTextViewModelFactory(
            llamaEngine = LlamaEngine(),
            preferencesManager = preferencesManager,
            diagnosticsManager = diagnosticsManager
        )
    )
    
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Process text on launch
    LaunchedEffect(selectedText) {
        if (selectedText.isNotEmpty()) {
            viewModel.processText(selectedText)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Crispify") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    uiState.isProcessing -> {
                        // Show loading state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Simplifying text...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    uiState.error != null -> {
                        // Show error state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = uiState.error ?: "An error occurred",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    else -> {
                        // Show processed text
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                if (uiState.processedText.isNotEmpty()) {
                                    Text(
                                        text = uiState.processedText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    Text(
                                        text = "No text to process",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        
                        // Action button
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.processedText))
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Text copied to clipboard",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.processedText.isNotEmpty()
                        ) {
                            Text("Copy to Clipboard")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}