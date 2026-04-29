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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundAnalyzerApp()
        }
    }
}

@Composable
fun SoundAnalyzerApp() {
    var hasPermission by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Tap to start") }
    var freq by remember { mutableStateOf("--") }
    var wave by remember { mutableStateOf("--") }
    
    val context = LocalContext.current
    
    // Simple permission request using ActivityResultContracts
    val requestPermission = remember {
        ActivityResultContracts.RequestPermission()    }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = requestPermission
    ) { granted ->
        if (granted) {
            hasPermission = true
            status = "Listening"
            AudioProcessor.startAnalysis { f, w ->
                freq = f.toInt().toString()
                wave = String.format("%.1f", w)
            }
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
                            status = "Listening"
                            AudioProcessor.startAnalysis { f, w ->
                                freq = f.toInt().toString()
                                wave = String.format("%.1f", w)
                            }
                        } else {
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {                    Text("Allow Microphone")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(status)
                Text("Frequency: " + freq + " Hz")
                Text("Wavelength: " + wave + " m")
                
                if (hasPermission) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            hasPermission = false
                            status = "Tap to start"
                            freq = "--"
                            wave = "--"
                            AudioProcessor.stopAnalysis()
                        }
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
    }
}
