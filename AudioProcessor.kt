package com.example.soundanalyzer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.*

object AudioProcessor {
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 2048 // Power of 2 for clean FFT bins
    private const val SPEED_OF_SOUND = 343.0 // m/s at 20°C
    
    private var audioRecord: AudioRecord? = null
    private var analysisJob: Job? = null
    private var isAnalyzing = false

    fun startAnalysis(
        onResult: (AnalysisResult, DoubleArray) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isAnalyzing) return
        isAnalyzing = true
        
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
            onError("Failed to initialize microphone")
            isAnalyzing = false
            return
        }
        
        // Start analysis coroutine
        analysisJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                val buffer = ShortArray(BUFFER_SIZE)
                                while (isAnalyzing) {
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                    if (read > 0) {
                        val result = analyzeBuffer(buffer, SAMPLE_RATE)
                        withContext(Dispatchers.Main) {
                            onResult(result.first, result.second)
                        }
                    }
                    // Small delay to avoid overwhelming UI
                    delay(100)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Analysis error")
                }
            } finally {
                audioRecord?.stop()
            }
        }
    }

    fun stopAnalysis() {
        isAnalyzing = false
        analysisJob?.cancel()
        audioRecord?.release()
        audioRecord = null
    }

    private fun analyzeBuffer(buffer: ShortArray, sampleRate: Int): Pair<AnalysisResult, DoubleArray> {
        // Convert to normalized doubles [-1.0, 1.0]
        val signal = DoubleArray(buffer.size) { buffer[it] / 32767.0 }
        
        // Apply Hanning window to reduce spectral leakage
        for (i in signal.indices) {
            signal[i] *= 0.5 * (1 - cos(2 * PI * i / (signal.size - 1)))
        }
        
        // Compute DFT (simplified for 2048 samples - acceptable for demo)
        val magnitude = computeMagnitudeSpectrum(signal)
        
        // Calculate RMS amplitude in dBFS
        val rms = sqrt(signal.map { it * it }.average())
        val amplitudeDb = if (rms > 1e-10) 20.0 * log10(rms) else -120.0
        
        // Find dominant frequency (peak in spectrum)
        var maxIdx = 0
        var maxMag = 0.0
        for (i in 1 until magnitude.size / 2) { // Skip DC component
            if (magnitude[i] > maxMag) {
                maxMag = magnitude[i]                maxIdx = i
            }
        }
        val dominantFreq = maxIdx * sampleRate.toDouble() / signal.size
        
        // Calculate wavelength: λ = v / f
        val wavelength = if (dominantFreq > 20) SPEED_OF_SOUND / dominantFreq else Double.POSITIVE_INFINITY
        
        val result = AnalysisResult(
            amplitude = amplitudeDb,
            dominantFrequency = dominantFreq,
            wavelength = wavelength,
            sampleRate = sampleRate
        )
        
        return Pair(result, magnitude)
    }

    private fun computeMagnitudeSpectrum(signal: DoubleArray): DoubleArray {
        val n = signal.size
        val magnitude = DoubleArray(n / 2)
        
        // Naive DFT: O(n²) but works for n=2048 on modern devices
        for (k in 0 until n / 2) {
            var realSum = 0.0
            var imagSum = 0.0
            for (t in 0 until n) {
                val angle = -2 * PI * k * t / n
                realSum += signal[t] * cos(angle)
                imagSum += signal[t] * sin(angle)
            }
            magnitude[k] = sqrt(realSum * realSum + imagSum * imagSum) / n
        }
        return magnitude
    }
}