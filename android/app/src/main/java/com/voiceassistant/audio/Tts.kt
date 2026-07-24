package com.voiceassistant.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Обгортка системного TextToSpeech для озвучування українською.
 *
 * РИЗИК (plan §4.5): український голос є не на всіх пристроях. Після init
 * перевіряємо isUkrainianAvailable — якщо ні, ViewModel покаже текст на екрані
 * і попередить (резерв — хмарний TTS, поза MVP).
 */
class Tts(context: Context, private val onReady: (ukAvailable: Boolean) -> Unit) {

    private var tts: TextToSpeech? = null
    private var ukAvailable = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale("uk", "UA"))
                ukAvailable = res != TextToSpeech.LANG_MISSING_DATA &&
                    res != TextToSpeech.LANG_NOT_SUPPORTED
            }
            onReady(ukAvailable)
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    fun isUkrainianAvailable(): Boolean = ukAvailable

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
