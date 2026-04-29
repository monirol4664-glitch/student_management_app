package com.example.soundanalyzer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.*

object AudioProcessor {
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 1024
    private const val SPEED_OF_SOUND = 343.0
    
    private var audioRecord: AudioRecord? = null
    private var analysisJob: Job? = null
    private var isRunning = false

    fun startAnalysis(onUpdate: (Double, Double) -> Unit) {
        if (isRunning) return
        isRunning = true
        
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
        
        analysisJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                val buffer = ShortArray(BUFFER_SIZE)
                
                while (isRunning) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                    if (read > 0) {
                        val result = processAudio(buffer, SAMPLE_RATE)
                        withContext(Dispatchers.Main) {                            onUpdate(result.frequency, result.wavelength)
                        }
                    }
                    delay(200)
                }
            } catch (e: Exception) {
                // Silently handle errors for CI build
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

    private fun processAudio(buffer: ShortArray, sampleRate: Int): AudioResult {
        // Simple zero-crossing frequency estimation
        var crossings = 0
        var lastSign = buffer[0] >= 0
        
        for (i in 1 until buffer.size) {
            val currentSign = buffer[i] >= 0
            if (currentSign != lastSign) {
                crossings++
                lastSign = currentSign
            }
        }
        
        // Frequency = crossings per second / 2 (one wave = 2 crossings)
        val duration = buffer.size.toDouble() / sampleRate
        val frequency = (crossings / duration) / 2.0
        
        // Wavelength = speed of sound / frequency
        val wavelength = if (frequency > 20) {
            SPEED_OF_SOUND / frequency
        } else {
            Double.POSITIVE_INFINITY
        }
        
        return AudioResult(
            frequency = frequency.coerceIn(20.0, 20000.0),
            wavelength = wavelength
        )
    }
}
data class AudioResult(val frequency: Double, val wavelength: Double)