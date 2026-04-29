package com.example.soundanalyzer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

object AudioProcessor {
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 2048
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
            if (minBuf > BUFFER_SIZE * 2) minBuf else BUFFER_SIZE * 2
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
                    val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: -1
                    
                    if (read >= BUFFER_SIZE) {
                        var crossings = 0
                        var lastPos = buffer[0] >= 0
                        
                        for (i in 1 until buffer.size) {
                            val currPos = buffer[i] >= 0
                            if (currPos != lastPos) {
                                crossings++
                                lastPos = currPos
                            }
                        }
                        
                        val duration = BUFFER_SIZE.toDouble() / SAMPLE_RATE
                        var freq = 0.0
                        if (crossings > 0) {
                            freq = (crossings / duration) / 2.0
                        }
                        
                        if (freq < 20) freq = 20.0
                        if (freq > 20000) freq = 20000.0
                        
                        var wave = Double.POSITIVE_INFINITY
                        if (freq > 20) {
                            wave = SPEED_OF_SOUND / freq
                        }
                        
                        withContext(Dispatchers.Main) {
                            onUpdate(freq, wave)
                        }
                    }
                    delay(100)
                }
            } catch (e: Exception) {
                // Handle silently
            } finally {
                try { audioRecord?.stop() } catch (e: Exception) {}
            }
        }
    }

    fun stopAnalysis() {
        isRunning = false
        analysisJob?.cancel()
        try { audioRecord?.release() } catch (e: Exception) {}
        audioRecord = null
    }
}