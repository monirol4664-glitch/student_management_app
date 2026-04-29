package com.example.soundanalyzer

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import kotlin.math.sqrt

object AudioProcessor {
    /**
     * Analyzes PCM 16-bit audio buffer offline using JTransforms.
     * Returns normalized frequency magnitudes for visualization.
     */
    fun analyze(buffer: ShortArray, sampleRate: Int): Spectrum {
        val size = buffer.size
        val fftData = DoubleArray(size) { buffer[it] / 32767.0 }
        
        val fft = DoubleFFT_1D(size.toLong())
        fft.realForward(fftData)

        val magnitudes = DoubleArray(size / 2)
        for (i in magnitudes.indices) {
            val real = fftData[2 * i]
            val imag = if (i == 0 || i == magnitudes.size - 1) 0.0 else fftData[2 * i + 1]
            magnitudes[i] = sqrt(real * real + imag * imag) / size
        }
        return Spectrum(magnitudes, sampleRate)
    }

    data class Spectrum(val magnitudes: DoubleArray, val sampleRate: Int)
}
