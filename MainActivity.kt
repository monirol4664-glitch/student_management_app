package com.example.soundanalyzer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> /* Handled via state in Compose */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // ✅ All state MUST be inside setContent (Composable context)
            var hasPermission by remember { mutableStateOf(false) }
            var statusText by remember { mutableStateOf("Tap to start") }
            var frequencyText by remember { mutableStateOf("Frequency: -- Hz") }
            var wavelengthText by remember { mutableStateOf("Wavelength: -- m") }
            
            val context = LocalContext.current
            
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🎙️ Sound Analyzer",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    hasPermission = true
                                    statusText = "Listening..."
                                    // AudioProcessor.startAnalysis(...) would go here
                                } else {
                                    requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            enabled = !hasPermission
                        ) {
                            Text("🔊 Allow Microphone")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = statusText, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = frequencyText, style = MaterialTheme.typography.bodyMedium)
                        Text(text = wavelengthText, style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Offline • No internet required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // AudioProcessor.stopAnalysis()
    }
}