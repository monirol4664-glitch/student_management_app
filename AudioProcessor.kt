package com.example.soundanalyzer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.*

object AudioProcessor {
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 2048
    private const val SPEED_OF_SOUND = 343.0
    private const val TAG = "SoundProcessor"
    
    private var audioRecord: AudioRecord? = null
    private var analysisJob: Job? = null
    private var isRunning = false

    fun startAnalysis(
        onUpdate: (Double, Double) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRunning) {
            Log.w(TAG, "Already running")
            return
        }
        isRunning = true
        Log.d(TAG, "Starting analysis")
        
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        Log.d(TAG, "Min buffer: $minBuf, using: ${maxOf(minBuf, BUFFER_SIZE * 2)}")
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, BUFFER_SIZE * 2)
        )
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            onError("Microphone unavailable")
            isRunning = false
            return        }
        
        analysisJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                Log.d(TAG, "Recording started")
                
                val buffer = ShortArray(BUFFER_SIZE)
                var silentCount = 0
                
                while (isRunning) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                    
                    if (read >= BUFFER_SIZE) {
                        // Check if buffer has actual sound (not just silence)
                        var maxAmp = 0
                        for (sample in buffer) {
                            val amp = abs(sample.toInt())
                            if (amp > maxAmp) maxAmp = amp
                        }
                        
                        if (maxAmp < 100) {
                            // Very quiet - don't spam updates
                            silentCount++
                            if (silentCount % 10 == 0) {
                                withContext(Dispatchers.Main) {
                                    onUpdate(0.0, Double.POSITIVE_INFINITY)
                                }
                            }
                            delay(100)
                            continue
                        }
                        silentCount = 0
                        
                        val result = analyzeBuffer(buffer, SAMPLE_RATE)
                        Log.d(TAG, "Analyzed: ${result.frequency}Hz, crossings=${result.crossings}")
                        
                        withContext(Dispatchers.Main) {
                            onUpdate(result.frequency, result.wavelength)
                        }
                    } else if (read < 0) {
                        Log.e(TAG, "AudioRecord read error: $read")
                        break
                    }
                    
                    delay(50) // Update ~20x/sec max
                }
            } catch (e: Exception) {
                Log.e(TAG, "Analysis error", e)
                withContext(Dispatchers.Main) {                    onError("Error: ${e.message}")
                }
            } finally {
                try { audioRecord?.stop() } catch (_: Exception) {}
                Log.d(TAG, "Recording stopped")
            }
        }
    }

    fun stopAnalysis() {
        Log.d(TAG, "Stopping analysis")
        isRunning = false
        analysisJob?.cancel()
        audioRecord?.release()
        audioRecord = null
    }

    private fun analyzeBuffer(buffer: ShortArray, sampleRate: Int): AudioResult {
        // Convert to normalized signal
        val signal = DoubleArray(buffer.size) { buffer[it] / 32767.0 }
        
        // Remove DC offset with simple high-pass
        var prev = 0.0
        for (i in signal.indices) {
            val curr = signal[i]
            signal[i] = curr - prev
            prev = curr
        }
        
        // Count zero crossings
        var crossings = 0
        var lastPositive = signal[0] > 0
        
        for (i in 1 until signal.size) {
            val currentPositive = signal[i] > 0
            if (currentPositive != lastPositive) {
                crossings++
                lastPositive = currentPositive
            }
        }
        
        // Calculate frequency
        val duration = buffer.size.toDouble() / sampleRate
        var frequency = if (crossings > 0) {
            (crossings / duration) / 2.0
        } else {
            0.0
        }
        
        // Clamp to audible range        frequency = frequency.coerceIn(20.0, 20000.0)
        
        // Calculate wavelength
        val wavelength = if (frequency > 20) {
            SPEED_OF_SOUND / frequency
        } else {
            Double.POSITIVE_INFINITY
        }
        
        return AudioResult(frequency, wavelength, crossings)
    }
}

data class AudioResult(val frequency: Double, val wavelength: Double, val crossings: Int = 0)