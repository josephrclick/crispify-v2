package com.clickapps.crispify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.clickapps.crispify.data.PreferencesManager
import com.clickapps.crispify.ui.onboarding.FirstLaunchRoute
import com.clickapps.crispify.ui.theme.CrispifyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        preferencesManager = PreferencesManager(this)
        
        setContent {
            CrispifyTheme {
                var showFirstLaunch by remember { mutableStateOf(false) }
                
                // Check first launch status
                LaunchedEffect(Unit) {
                    showFirstLaunch = preferencesManager.isFirstLaunch.first()
                }
                
                when {
                    showFirstLaunch -> {
                        FirstLaunchRoute(
                            onDismiss = {
                                // After first launch, close the app
                                // User will access Crispify through text selection menu
                                finish()
                            }
                        )
                    }
                    else -> {
                        // Show main content for regular app launches
                        MainContent(
                            onClose = { finish() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Main content shown when app is launched directly (not via text selection)
 * Displays instructions and a close button
 */
@Composable
fun MainContent(
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Crispify",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "How to use Crispify:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "1. Select any text in any app\n" +
                          "2. Look for 'Crispify' in the selection menu\n" +
                          "3. Tap to simplify the selected text",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Crispify runs entirely on your device.\nYour text never leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}