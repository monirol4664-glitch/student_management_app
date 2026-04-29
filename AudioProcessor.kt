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

object AudioProcessor {
    private const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 2048
    private const val SPEED_OF_SOUND = 343.0
    
    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private var running = false

    fun startAnalysis(onUpdate: (Double, Double) -> Unit) {
        if (running) return
        running = true
        
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, if (minBuf > BUFFER_SIZE * 2) minBuf else BUFFER_SIZE * 2)
        
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) { running = false; return }
        
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord?.startRecording()
                val buf = ShortArray(BUFFER_SIZE)
                while (running) {
                    val read = audioRecord?.read(buf, 0, BUFFER_SIZE) ?: -1
                    if (read >= BUFFER_SIZE) {
                        var cross = 0
                        var last = buf[0] >= 0
                        for (i in 1 until buf.size) {
                            val curr = buf[i] >= 0
                            if (curr != last) { cross++; last = curr }
                        }
                        val dur = BUFFER_SIZE.toDouble() / SAMPLE_RATE
                        var f = if (cross > 0) (cross / dur) / 2.0 else 20.0
                        if (f < 20) f = 20.0; if (f > 20000) f = 20000.0
                        val w = if (f > 20) SPEED_OF_SOUND / f else 999.9
                        withContext(Dispatchers.Main) { onUpdate(f, w) }
                    }
                    delay(100)
                }
            } catch (e: Exception) {}
            finally { try { audioRecord?.stop() } catch (e: Exception) {} }
        }
    }

    fun stopAnalysis() { running = false; job?.cancel(); try { audioRecord?.release() } catch (e: Exception) {}; audioRecord = null }
}