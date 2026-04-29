package com.example.soundanalyzer

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private var isRecording by mutableStateOf(false)
    private var analysisResult by mutableStateOf<AnalysisResult?>(null)
    private var spectrumData by mutableStateOf<DoubleArray?>(null)
    
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎙️ Offline Sound Analyzer", 
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 8.dp))
                                                // Permission button or recording UI
                        if (!isRecording) {
                            Button(
                                onClick = {
                                    val context = LocalContext.current
                                    if (ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        startRecording()
                                    } else {
                                        requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                Text("🔊 Allow Microphone & Start")
                            }
                        } else {
                            // Live analysis display
                            analysisResult?.let { result ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("📊 Live Analysis", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("🔊 Amplitude: ${String.format("%.1f", result.amplitude)} dB")
                                        Text("🎵 Dominant Frequency: ${String.format("%.0f", result.dominantFrequency)} Hz")
                                        Text("🌊 Wavelength: ${String.format("%.2f", result.wavelength)} m")
                                        Text("⚡ Sample Rate: ${result.sampleRate} Hz")
                                    }
                                }
                            }
                            
                            // Simple spectrum visualization
                            spectrumData?.let { data ->
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .padding(vertical = 8.dp)
                                ) {
                                    val width = size.width
                                    val height = size.height
                                    val maxVal = data.maxOrNull() ?: 1.0
                                                                        // Draw baseline
                                    drawLine(
                                        color = Color.Gray,
                                        start = Offset(0f, height / 2),
                                        end = Offset(width, height / 2),
                                        strokeWidth = 1f
                                    )
                                    
                                    // Draw spectrum bars
                                    val barWidth = width / data.size.coerceAtLeast(64)
                                    for (i in 0 until data.size.coerceAtMost(64)) {
                                        val magnitude = (data[i] / maxVal).coerceIn(0.0, 1.0).toFloat()
                                        val barHeight = magnitude * height / 2
                                        drawLine(
                                            color = Color.Cyan,
                                            start = Offset(i * barWidth, height / 2),
                                            end = Offset(i * barWidth, height / 2 - barHeight),
                                            strokeWidth = barWidth - 1f
                                        )
                                    }
                                }
                            }
                            
                            // Stop button
                            Button(
                                onClick = { stopRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("⏹️ Stop Analysis")
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Offline • No internet required", 
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray)
                    }
                }
            }
        }
    }

    private fun startRecording() {
        isRecording = true
        // Launch audio processing in background
        lifecycleScope.launch {
            AudioProcessor.startAnalysis(
                onResult = { result, spectrum ->
                    analysisResult = result                    spectrumData = spectrum
                },
                onError = { /* Handle error silently for CI build */ }
            )
        }
    }

    private fun stopRecording() {
        isRecording = false
        AudioProcessor.stopAnalysis()
        analysisResult = null
        spectrumData = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) stopRecording()
    }
}

data class AnalysisResult(
    val amplitude: Double,
    val dominantFrequency: Double,
    val wavelength: Double,
    val sampleRate: Int
)