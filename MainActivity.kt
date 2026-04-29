package com.example.soundanalyzer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission handled via state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundAnalyzerScreen(requestPermission)
        }
    }
}

@Composable
fun SoundAnalyzerScreen(requestPermission: ActivityResultContracts.RequestPermission) {
    var hasPermission by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Tap to start") }
    var freqText by remember { mutableStateOf("Frequency: -- Hz") }    var waveText by remember { mutableStateOf("Wavelength: -- m") }
    
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
                    text = "Sound Analyzer",
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
                            statusText = "Listening"
                            AudioProcessor.startAnalysis { freq, wave ->
                                freqText = "Frequency: " + freq.toInt().toString() + " Hz"
                                waveText = "Wavelength: " + String.format("%.2f", wave) + " m"
                            }
                        } else {
                            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Text(text = "Allow Microphone")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = statusText)
                Text(text = freqText)
                Text(text = waveText)
                
                if (hasPermission) {
                    Spacer(modifier = Modifier.height(16.dp))                    Button(
                        onClick = {
                            hasPermission = false
                            statusText = "Tap to start"
                            freqText = "Frequency: -- Hz"
                            waveText = "Wavelength: -- m"
                            AudioProcessor.stopAnalysis()
                        }
                    ) {
                        Text(text = "Stop")
                    }
                }
            }
        }
    }
}