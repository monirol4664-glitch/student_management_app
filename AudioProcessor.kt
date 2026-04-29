package com.example.soundanalyzer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.*

object AudioProcessor {
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 2048  // ~46ms of audio
    private const val SPEED_OF_SOUND = 343.0  // m/s at 20°C
    
    private var audioRecord: AudioRecord? = null
    private var analysisJob: Job? = null
    private var isRunning = false

    fun startAnalysis(onUpdate: (Double, Double) -> Unit) {
        if (isRunning) return
        isRunning = true
        
        // Initialize AudioRecord
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, BUFFER_SIZE * 2)
        )
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            isRunning = false
            return
        }
        
        // Start analysis in background
        analysisJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                val buffer = ShortArray(BUFFER_SIZE)
                
                while (isRunning) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                    if (read >= BUFFER_SIZE) {                        val result = analyzeBuffer(buffer, SAMPLE_RATE)
                        withContext(Dispatchers.Main) {
                            onUpdate(result.frequency, result.wavelength)
                        }
                    }
                    // Small delay to avoid UI overload
                    delay(100)
                }
            } catch (e: Exception) {
                // Silently handle errors in production
            } finally {
                audioRecord?.stop()
            }
        }
    }

    fun stopAnalysis() {
        isRunning = false
        analysisJob?.cancel()
        audioRecord?.release()
        audioRecord = null
    }

    private fun analyzeBuffer(buffer: ShortArray, sampleRate: Int): AudioResult {
        // Convert to normalized signal [-1.0, 1.0]
        val signal = DoubleArray(buffer.size) { buffer[it] / 32767.0 }
        
        // Apply simple high-pass filter to remove DC offset
        var prev = 0.0
        for (i in signal.indices) {
            val curr = signal[i]
            signal[i] = curr - prev
            prev = curr
        }
        
        // Zero-crossing frequency estimation
        var crossings = 0
        var lastPositive = signal[0] > 0
        
        for (i in 1 until signal.size) {
            val currentPositive = signal[i] > 0
            if (currentPositive != lastPositive) {
                crossings++
                lastPositive = currentPositive
            }
        }
        
        // Calculate frequency: crossings per second / 2
        val duration = buffer.size.toDouble() / sampleRate
        var frequency = (crossings / duration) / 2.0        
        // Clamp to audible range
        frequency = frequency.coerceIn(20.0, 20000.0)
        
        // Calculate wavelength: λ = v / f
        val wavelength = if (frequency > 20) {
            SPEED_OF_SOUND / frequency
        } else {
            Double.POSITIVE_INFINITY
        }
        
        return AudioResult(frequency, wavelength)
    }
}

data class AudioResult(val frequency: Double, val wavelength: Double)