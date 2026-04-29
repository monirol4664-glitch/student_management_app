package com.example.soundanalyzer

import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.PI

object AudioProcessor {
    /**
     * Analyzes PCM 16-bit audio buffer using pure Kotlin DFT.
     * No external dependencies - compiles 100% offline.
     * For production: replace with optimized FFT library later.
     */
    fun analyze(buffer: ShortArray, sampleRate: Int): Spectrum {
        val size = buffer.size.coerceAtMost(1024) // Limit for performance
        val fftSize = 1024 // Power of 2 for clean frequency bins
        
        // Convert to normalized doubles
        val real = DoubleArray(fftSize)
        val imag = DoubleArray(fftSize)
        for (i in 0 until size) {
            real[i] = buffer[i] / 32767.0
        }
        
        // Apply Hanning window
        for (i in 0 until fftSize) {
            val window = 0.5 * (1 - cos(2 * PI * i / (fftSize - 1)))
            real[i] *= window
        }
        
        // Simple DFT (O(n²) but works for 1024 samples)
        val magnitudes = DoubleArray(fftSize / 2)
        for (k in 0 until magnitudes.size) {
            var sumReal = 0.0
            var sumImag = 0.0
            for (n in 0 until fftSize) {
                val angle = -2 * PI * k * n / fftSize
                sumReal += real[n] * cos(angle)
                sumImag += real[n] * kotlin.math.sin(angle)
            }
            magnitudes[k] = sqrt(sumReal * sumReal + sumImag * sumImag) / fftSize
        }
        
        return Spectrum(magnitudes, sampleRate)
    }

    data class Spectrum(val magnitudes: DoubleArray, val sampleRate: Int)
}