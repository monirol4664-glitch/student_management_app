package com.example.soundanalyzer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.log10
import kotlin.math.sqrt

class AudioProcessor(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2
) {
    private val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
    )

    private val fft = DoubleFFT_1D(bufferSize.toLong())
    private val pcmBuffer = ShortArray(bufferSize)

    fun startAnalysis(): Flow<SpectrumData> = flow {
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord not initialized")
        }
        audioRecord.startRecording()

        while (true) {
            val read = audioRecord.read(pcmBuffer, 0, bufferSize)
            if (read > 0) {
                val spectrum = processBuffer(pcmBuffer)
                emit(spectrum)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun stop() {
        if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop()
        }
    }

    private fun processBuffer(shorts: ShortArray): SpectrumData {
        val doubles = DoubleArray(shorts.size) { shorts[it] / 32767.0 }
        applyHanningWindow(doubles)

        val fftData = doubles.copyOf()
        fft.realForward(fftData)

        val magnitude = DoubleArray(doubles.size / 2)
        var maxMag = 0.0
        for (i in magnitude.indices) {
            val real = fftData[2 * i]
            val imag = if (i == 0 || i == magnitude.size - 1) 0.0 else fftData[2 * i + 1]
            magnitude[i] = sqrt(real * real + imag * imag) / doubles.size
            if (magnitude[i] > maxMag) maxMag = magnitude[i]
        }

        val rms = sqrt(doubles.map { it * it }.average())
        val dbfs = if (rms > 1e-10) 20.0 * log10(rms) else -120.0

        return SpectrumData(magnitude, dbfs, sampleRate)
    }

    private fun applyHanningWindow(data: DoubleArray) {
        for (i in data.indices) {
            data[i] *= 0.5 * (1 - kotlin.math.cos(2.0 * Math.PI * i / (data.size - 1)))
        }
    }

    data class SpectrumData(
        val magnitude: DoubleArray,
        val dbfs: Double,
        val sampleRate: Int
    )
}
