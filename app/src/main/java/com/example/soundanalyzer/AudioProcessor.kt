package com.example.soundanalyzer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.*

object AudioProcessor {
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 2048
    private const val SPEED_OF_SOUND = 343.0
    
    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private var running = false

    fun startAnalysis(onUpdate: (AudioResult) -> Unit) {
        if (running) return
        running = true
        
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, if (minBuf > BUFFER_SIZE * 2) minBuf else BUFFER_SIZE * 2
        )
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) { running = false; return }
        
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                val buffer = ShortArray(BUFFER_SIZE)
                
                while (running) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                    if (read >= BUFFER_SIZE) {
                        val result = analyze(buffer, SAMPLE_RATE)
                        withContext(Dispatchers.Main) { onUpdate(result) }
                    }
                    delay(50) // ~20 updates/sec
                }
            } catch (e: Exception) {}
            finally { try { audioRecord?.stop() } catch (e: Exception) {} }
        }
    }

    fun stopAnalysis() {
        running = false; job?.cancel()
        try { audioRecord?.release() } catch (e: Exception) {}        audioRecord = null
    }

    private fun analyze(buffer: ShortArray, sampleRate: Int): AudioResult {
        // Convert to normalized float array [-1.0, 1.0]
        val signal = FloatArray(buffer.size) { buffer[it] / 32767f }
        
        // Remove DC offset (simple high-pass)
        var prev = 0f
        for (i in signal.indices) { val curr = signal[i]; signal[i] = curr - prev; prev = curr }
        
        // Calculate amplitude metrics
        var sumSq = 0f; var peak = 0f
        for (s in signal) {
            sumSq += s * s
            val abs = kotlin.math.abs(s)
            if (abs > peak) peak = abs
        }
        val rms = sqrt(sumSq / signal.size)
        val amplitudeDb = if (rms > 1e-5f) 20f * log10(rms) else -120f
        
        // Zero-crossing analysis
        var crossings = 0; var lastPos = signal[0] > 0
        for (i in 1 until signal.size) {
            val currPos = signal[i] > 0
            if (currPos != lastPos) { crossings++; lastPos = currPos }
        }
        val duration = signal.size / sampleRate.toFloat()
        val zeroCrossingRate = crossings / duration
        var frequency = if (crossings > 0) (crossings / duration) / 2f else 20f
        frequency = frequency.coerceIn(20f, 20000f)
        val wavelength = if (frequency > 20) SPEED_OF_SOUND / frequency else 999.9f
        
        // Prepare waveform for visualization (downsample to 128 points for Canvas)
        val waveform = FloatArray(128)
        val step = signal.size / 128
        for (i in 0 until 128) waveform[i] = signal[i * step]
        
        // Simple spectrum estimate (magnitude of first 32 frequency bins)
        val spectrum = FloatArray(32)
        for (k in 0 until 32) {
            var real = 0f; var imag = 0f
            for (t in signal.indices) {
                val angle = -2f * PI.toFloat() * k * t / signal.size
                real += signal[t] * cos(angle)
                imag += signal[t] * sin(angle)
            }
            spectrum[k] = sqrt(real * real + imag * imag) / signal.size
        }
                return AudioResult(
            frequency = frequency.toDouble(),
            wavelength = wavelength.toDouble(),
            amplitudeDb = amplitudeDb.toDouble(),
            rms = rms.toDouble(),
            peak = peak.toDouble(),
            zeroCrossingRate = zeroCrossingRate.toDouble(),
            waveform = waveform,
            spectrum = spectrum
        )
    }
}

data class AudioResult(
    val frequency: Double,
    val wavelength: Double,
    val amplitudeDb: Double,
    val rms: Double,
    val peak: Double,
    val zeroCrossingRate: Double,
    val waveform: FloatArray,
    val spectrum: FloatArray
)