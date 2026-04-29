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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SoundAnalyzerApp() }
    }
}

@Composable
fun SoundAnalyzerApp() {
    var hasPermission by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Tap to start") }
    
    // Audio metrics
    var frequency by remember { mutableStateOf("--") }
    var wavelength by remember { mutableStateOf("--") }
    var amplitudeDb by remember { mutableStateOf("--") }
    var rmsLevel by remember { mutableStateOf("--") }
    var peakAmp by remember { mutableStateOf("--") }
    var zcr by remember { mutableStateOf("--") }
    
    // Visualization data
    var waveform by remember { mutableStateOf<FloatArray?>(null) }
    var spectrum by remember { mutableStateOf<FloatArray?>(null) }
    
    val context = LocalContext.current
    
    val requestPermission = remember { ActivityResultContracts.RequestPermission() }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = requestPermission    ) { granted ->
        if (granted) {
            hasPermission = true
            status = "Listening"
            AudioProcessor.startAnalysis { result ->
                frequency = String.format("%.0f", result.frequency)
                wavelength = if (result.wavelength > 100) "N/A" else String.format("%.2f", result.wavelength)
                amplitudeDb = String.format("%.1f", result.amplitudeDb)
                rmsLevel = String.format("%.3f", result.rms)
                peakAmp = String.format("%.3f", result.peak)
                zcr = String.format("%.0f", result.zeroCrossingRate)
                waveform = result.waveform
                spectrum = result.spectrum
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text("🎙️ Sound Analyzer", style = MaterialTheme.typography.titleLarge)
                Text(status, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                // Metrics Grid
                if (hasPermission) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MetricCard("Freq", frequency + " Hz", Color.Cyan)
                        MetricCard("Wave", wavelength + " m", Color.Magenta)
                        MetricCard("Amp", amplitudeDb + " dB", Color.Yellow)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MetricCard("RMS", rmsLevel, Color.Green)
                        MetricCard("Peak", peakAmp, Color.Red)
                        MetricCard("ZCR", zcr + "/s", Color.Blue)
                    }
                }
                
                // Waveform Visualization
                if (waveform != null) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(80.dp).padding(vertical = 4.dp)) {
                        val w = size.width; val h = size.height
                        val data = waveform!!
                        if (data.isNotEmpty()) {
                            val path = Path()
                            val mid = h / 2
                            val scale = h / 2.5f                            path.moveTo(0f, mid - data[0] * scale)
                            for (i in 1 until data.size) {
                                val x = i * w / data.size
                                val y = mid - data[i] * scale
                                path.lineTo(x, y)
                            }
                            drawPath(path, Color.Cyan, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
                            // Draw baseline
                            drawLine(Color.Gray.copy(alpha = 0.5f), Offset(0f, mid), Offset(w, mid), 1f)
                        }
                    }
                }
                
                // Spectrum Bars (simplified)
                if (spectrum != null) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).padding(vertical = 4.dp)) {
                        val w = size.width; val h = size.height
                        val data = spectrum!!
                        val max = data.maxOrNull() ?: 1f
                        val barW = w / 32f
                        for (i in 0 until 32) {
                            val mag = (data[i] / max).coerceIn(0f, 1f)
                            val barH = mag * h
                            drawRect(
                                color = Color(0xFF00BCD4).copy(alpha = 0.5f + mag * 0.5f),
                                topLeft = Offset(i * barW, h - barH),
                                size = androidx.compose.ui.geometry.Size(barW - 1f, barH)
                            )
                        }
                    }
                }
                
                // Control Button
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            hasPermission = !hasPermission
                            if (hasPermission) {
                                status = "Listening"
                                AudioProcessor.startAnalysis { result ->
                                    frequency = String.format("%.0f", result.frequency)
                                    wavelength = if (result.wavelength > 100) "N/A" else String.format("%.2f", result.wavelength)
                                    amplitudeDb = String.format("%.1f", result.amplitudeDb)
                                    rmsLevel = String.format("%.3f", result.rms)
                                    peakAmp = String.format("%.3f", result.peak)
                                    zcr = String.format("%.0f", result.zeroCrossingRate)
                                    waveform = result.waveform
                                    spectrum = result.spectrum
                                }
                            } else {                                status = "Tap to start"
                                AudioProcessor.stopAnalysis()
                                frequency = "--"; wavelength = "--"; amplitudeDb = "--"
                                rmsLevel = "--"; peakAmp = "--"; zcr = "--"
                                waveform = null; spectrum = null
                            }
                        } else {
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasPermission) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(if (hasPermission) "⏹️ Stop" else "🔊 Allow Microphone")
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Text("Offline • No internet", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)), modifier = Modifier.width(80.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}