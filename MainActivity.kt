package com.example.soundanalyzer

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var isRecording by mutableStateOf(false)
    private var amplitudeDb by mutableStateOf(0.0)
    private var dominantFreq by mutableStateOf(0.0)
    private var wavelength by mutableStateOf(Double.POSITIVE_INFINITY)
    private var spectrum by mutableStateOf<DoubleArray?>(null)
    
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startRecording() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎙️ Offline Sound Analyzer", 
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(vertical = 8.dp))
                        
                        if (!isRecording) {
                            Button(
                                onClick = {
                                    val ctx = LocalContext.current                                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) 
                                        == PackageManager.PERMISSION_GRANTED) {
                                        startRecording()
                                    } else {
                                        requestPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) { Text("🔊 Allow Mic & Start") }
                        } else {
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("📊 Live", style = MaterialTheme.typography.titleMedium)
                                    Text("🔊 ${String.format("%.1f", amplitudeDb)} dB")
                                    Text("🎵 ${String.format("%.0f", dominantFreq)} Hz")
                                    Text("🌊 ${if (wavelength.isFinite()) String.format("%.2f", wavelength) + " m" else "N/A"}")
                                }
                            }
                            spectrum?.let { data ->
                                Canvas(modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 8.dp)) {
                                    val w = size.width; val h = size.height
                                    val max = data.maxOrNull() ?: 1.0
                                    drawLine(Color.Gray, Offset(0f, h/2), Offset(w, h/2), 1f)
                                    val bw = w / 64f
                                    for (i in 0 until 64) {
                                        val m = (data[i] / max).coerceIn(0.0, 1.0).toFloat()
                                        drawLine(Color.Cyan, Offset(i*bw, h/2), Offset(i*bw, h/2 - m*h/2), bw-1f)
                                    }
                                }
                            }
                            Button(onClick = { stopRecording() }, 
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                Text("⏹️ Stop")
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text("Offline • No internet", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        }
    }

    private fun startRecording() {
        isRecording = true
        lifecycleScope.launch {
            AudioProcessor.startAnalysis(
                onResult = { amp, freq, wave, spec ->
                    amplitudeDb = amp; dominantFreq = freq; wavelength = wave; spectrum = spec
                }            )
        }
    }

    private fun stopRecording() {
        isRecording = false
        AudioProcessor.stopAnalysis()
        amplitudeDb = 0.0; dominantFreq = 0.0; wavelength = Double.POSITIVE_INFINITY; spectrum = null
    }

    override fun onDestroy() { super.onDestroy(); if (isRecording) stopRecording() }
}