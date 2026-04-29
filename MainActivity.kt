package com.example.soundanalyzer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
    ) { granted -> 
        Log.d("SoundApp", "Permission result: $granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var hasPermission by remember { mutableStateOf(false) }
            var statusText by remember { mutableStateOf("Tap to start") }
            var frequencyText by remember { mutableStateOf("Frequency: -- Hz") }
            var wavelengthText by remember { mutableStateOf("Wavelength: -- m") }
            
            val context = LocalContext.current
            
            // ✅ Start/stop audio when permission state changes
            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    Log.d("SoundApp", "Starting audio analysis")
                    AudioProcessor.startAnalysis(
                        onUpdate = { freq, wave ->
                            Log.d("SoundApp", "Update: ${freq}Hz, ${wave}m")
                            frequencyText = "Frequency: ${String.format("%.0f", freq)} Hz"
                            wavelengthText = "Wavelength: ${String.format("%.2f", wave)} m"
                            statusText = "🎙️ Listening..."
                        },
                        onError = { msg ->
                            Log.e("SoundApp", "Error: $msg")                            statusText = "❌ $msg"
                        }
                    )
                } else {
                    Log.d("SoundApp", "Stopping audio analysis")
                    AudioProcessor.stopAnalysis()
                    statusText = "Tap to start"
                    frequencyText = "Frequency: -- Hz"
                    wavelengthText = "Wavelength: -- m"
                }
            }
            
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
                                Log.d("SoundApp", "Button clicked")
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    Log.d("SoundApp", "Permission already granted")
                                    hasPermission = true
                                } else {
                                    Log.d("SoundApp", "Requesting permission")
                                    requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        ) {
                            Text(if (hasPermission) "🔄 Restart" else "🔊 Allow Microphone")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))                        Text(text = statusText, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = frequencyText, style = MaterialTheme.typography.bodyMedium)
                        Text(text = wavelengthText, style = MaterialTheme.typography.bodyMedium)
                        
                        if (hasPermission) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { hasPermission = false },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("⏹️ Stop")
                            }
                        }
                        
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
        AudioProcessor.stopAnalysis()
    }
}