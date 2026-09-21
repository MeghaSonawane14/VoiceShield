package com.voiceshield.ai.services

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioRecordManager(
    private val context: Context? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var recordJob: Job? = null
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isRecording = false

    private val _audioAmplitude = MutableStateFlow(0.0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHUNK_SIZE = 1600 // ~100ms chunks at 16kHz 16-bit
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        onAudioChunk: (ByteArray) -> Unit,
        onAmplitude: (Float) -> Unit = {}
    ) {
        if (isRecording) return

        val minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufSize, CHUNK_SIZE * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            _isCapturing.value = true

            recordJob = scope.launch {
                val buffer = ShortArray(CHUNK_SIZE / 2)
                val byteBuffer = ByteArray(CHUNK_SIZE)

                while (isActive && isRecording) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readCount > 0) {
                        // Compute RMS for live waveform
                        var sum = 0.0
                        for (i in 0 until readCount) {
                            val sample = buffer[i]
                            sum += sample * sample

                            // Convert short to little-endian bytes
                            byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                        }
                        val rms = sqrt(sum / readCount)
                        val normalizedAmplitude = (rms / 32768.0).toFloat().coerceIn(0.0f, 1.0f)
                        _audioAmplitude.value = normalizedAmplitude
                        onAmplitude(normalizedAmplitude)

                        // Send chunk to streaming handler
                        val chunkCopy = ByteArray(readCount * 2)
                        System.arraycopy(byteBuffer, 0, chunkCopy, 0, readCount * 2)
                        onAudioChunk(chunkCopy)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    fun startCapture(onChunkCaptured: (ByteArray) -> Unit) {
        startRecording(onAudioChunk = onChunkCaptured)
    }

    fun stopRecording() {
        isRecording = false
        _isCapturing.value = false
        _audioAmplitude.value = 0.0f
        recordJob?.cancel()
        recordJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            audioRecord = null
        }
    }

    fun stopCapture() {
        stopRecording()
    }
}
