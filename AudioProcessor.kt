package com.example.soundanalyzer

import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.PI

object AudioProcessor {
    /**
     * Pure Kotlin DFT implementation. Zero external dependencies.
     * Guaranteed to compile offline on any Android device.
     */
    fun analyze(buffer: ShortArray, sampleRate: Int): Spectrum {
        val n = minOf(buffer.size, 512)
        val magnitudes = DoubleArray(n / 2)

        for (k in magnitudes.indices) {
            var realSum = 0.0
            var imagSum = 0.0
            for (t in 0 until n) {
                val angle = -2 * PI * k * t / n
                val sample = buffer[t] / 32767.0
                realSum += sample * cos(angle)
                imagSum += sample * kotlin.math.sin(angle)
            }
            magnitudes[k] = sqrt(realSum * realSum + imagSum * imagSum) / n
        }
        return Spectrum(magnitudes, sampleRate)
    }

    data class Spectrum(val magnitudes: DoubleArray, val sampleRate: Int)
}