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

    fun startAnalysis(onResult: (Double, Double, Double, DoubleArray) -> Unit) {
        if (running) return
        running = true
        
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, BUFFER_SIZE * 2))
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) { running = false; return }
        
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                val buf = ShortArray(BUFFER_SIZE)
                while (running) {
                    val read = audioRecord?.read(buf, 0, BUFFER_SIZE) ?: 0
                    if (read > 0) {
                        val (amp, freq, wave, spec) = analyze(buf, SAMPLE_RATE)
                        withContext(Dispatchers.Main) { onResult(amp, freq, wave, spec) }
                    }
                    delay(100)
                }
            } finally { audioRecord?.stop() }
        }
    }

    fun stopAnalysis() {
        running = false
        job?.cancel()
        audioRecord?.release()
        audioRecord = null
    }

    private fun analyze(buf: ShortArray, sr: Int): Quad {
        val sig = DoubleArray(buf.size) { buf[it] / 32767.0 }
        for (i in sig.indices) sig[i] *= 0.5 * (1 - cos(2 * PI * i / (sig.size - 1)))
        
        val mag = DoubleArray(sig.size / 2)
        for (k in 0 until mag.size) {
            var r = 0.0; var im = 0.0
            for (t in sig.indices) {
                val a = -2 * PI * k * t / sig.size
                r += sig[t] * cos(a); im += sig[t] * sin(a)
            }
            mag[k] = sqrt(r*r + im*im) / sig.size
        }
        
        val rms = sqrt(sig.map { it*it }.average())
        val amp = if (rms > 1e-10) 20.0 * log10(rms) else -120.0
        
        var maxI = 1; var maxM = 0.0
        for (i in 1 until mag.size / 2) if (mag[i] > maxM) { maxM = mag[i]; maxI = i }
        val freq = maxI * sr.toDouble() / sig.size
        val wave = if (freq > 20) SPEED_OF_SOUND / freq else Double.POSITIVE_INFINITY
        
        return Quad(amp, freq, wave, mag)
    }
}

data class Quad(val amp: Double, val freq: Double, val wave: Double, val spec: DoubleArray)