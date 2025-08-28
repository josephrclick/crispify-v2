package com.clickapps.crispify.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clickapps.crispify.ui.theme.CrispifyTheme

/**
 * First Launch Screen for Crispify onboarding
 * Displays usage instructions, handles model initialization, and offers diagnostics opt-in
 *
 * @param isModelLoading Whether the AI model is currently being initialized
 * @param modelLoadingProgress Progress of model loading (0.0 to 1.0)
 * @param isDiagnosticsEnabled Current state of diagnostics preference
 * @param onDiagnosticsToggle Callback when diagnostics toggle is changed
 * @param onDismiss Callback when user dismisses the screen (after model loads)
 */
@Composable
fun FirstLaunchScreen(
    isModelLoading: Boolean,
    modelLoadingProgress: Float,
    isDiagnosticsEnabled: Boolean,
    onDiagnosticsToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Branding Placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.large
                    )
                    .testTag("BrandingPlaceholder"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name and Title
            Text(
                text = "Crispify",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI-powered text simplification",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Usage Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How to use Crispify",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "1. Select text in any app\n" +
                              "2. Tap the text selection menu\n" +
                              "3. Find and tap 'Crispify'\n" +
                              "4. Get simplified text instantly",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Model Loading Progress
            AnimatedVisibility(
                visible = isModelLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Preparing AI model...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { modelLoadingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ModelLoadingProgress"),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Diagnostics Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local Diagnostics",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Help improve Crispify (privacy-preserving)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Switch(
                        checked = isDiagnosticsEnabled,
                        onCheckedChange = onDiagnosticsToggle,
                        modifier = Modifier.testTag("DiagnosticsSwitch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Dismiss Button
            Button(
                onClick = onDismiss,
                enabled = !isModelLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = if (isModelLoading) "Please wait..." else "Get Started",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FirstLaunchScreenPreview() {
    CrispifyTheme {
        FirstLaunchScreen(
            isModelLoading = true,
            modelLoadingProgress = 0.6f,
            isDiagnosticsEnabled = false,
            onDiagnosticsToggle = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Loaded State")
@Composable
fun FirstLaunchScreenLoadedPreview() {
    CrispifyTheme {
        FirstLaunchScreen(
            isModelLoading = false,
            modelLoadingProgress = 1.0f,
            isDiagnosticsEnabled = true,
            onDiagnosticsToggle = {},
            onDismiss = {}
        )
    }
}