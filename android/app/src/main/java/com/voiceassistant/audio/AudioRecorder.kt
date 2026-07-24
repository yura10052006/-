package com.voiceassistant.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Запис аудіо через AudioRecord у форматі WAV: 8 кГц, моно, 16-bit PCM.
 *
 * 8 кГц навмисно (spec NFR-2) — це той самий рівень якості, що дає мікрофон
 * окулярів через HFP. Тестуємо STT одразу на «поганому» звуці, а не на чистому.
 *
 * VOICE_COMMUNICATION як джерело — вмикає ехо/шумоподавлення, ближче до сценарію
 * гарнітури.
 */
class AudioRecorder {

    private var recorder: AudioRecord? = null
    private var thread: Thread? = null
    @Volatile private var recording = false
    private val pcm = ByteArrayOutputStream()

    companion object {
        const val SAMPLE_RATE = 8000 // Гц
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }

    /** Починає запис. Потребує дозволу RECORD_AUDIO (перевіряється у ViewModel). */
    @SuppressLint("MissingPermission")
    fun start() {
        if (recording) return
        pcm.reset()

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        val bufSize = if (minBuf > 0) minBuf * 2 else SAMPLE_RATE * 2

        recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            CHANNEL,
            ENCODING,
            bufSize,
        )
        recorder?.startRecording()
        recording = true

        thread = Thread {
            val buffer = ByteArray(bufSize)
            while (recording) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) pcm.write(buffer, 0, read)
            }
        }.also { it.start() }
    }

    /** Зупиняє запис і повертає готовий WAV як байти. */
    fun stopAndGetWav(): ByteArray {
        recording = false
        thread?.join(500)
        thread = null
        recorder?.apply {
            try { stop() } catch (_: IllegalStateException) {}
            release()
        }
        recorder = null
        return pcmToWav(pcm.toByteArray(), SAMPLE_RATE, channels = 1, bitsPerSample = 16)
    }

    fun isRecording(): Boolean = recording

    /** Загортає сирий PCM у контейнер WAV (додає 44-байтовий заголовок). */
    private fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataLen = pcmData.size
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(36 + dataLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)                       // розмір fmt-блоку
        header.putShort(1)                       // PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataLen)
        return header.array() + pcmData
    }
}
