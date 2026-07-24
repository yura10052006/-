package com.voiceassistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.Config
import com.voiceassistant.audio.AudioRecorder
import com.voiceassistant.audio.BluetoothScoManager
import com.voiceassistant.audio.Tts
import com.voiceassistant.net.BackendClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Стани екрана (стейт-машина, plan §4.3). */
enum class UiState { IDLE, RECORDING, PROCESSING, SPEAKING, ERROR }

data class VoiceUi(
    val state: UiState = UiState.IDLE,
    val recognizedText: String = "",
    val replyText: String = "",
    val status: String = "Натисни й утримуй, щоб говорити",
    val ttsWarning: String? = null,
)

/**
 * Оркеструє цикл: кнопка -> запис -> бекенд -> TTS. Клієнт лишається «дурним».
 */
class VoiceViewModel(app: Application) : AndroidViewModel(app) {

    private val recorder = AudioRecorder()
    private val sco = BluetoothScoManager(app)
    private val backend = BackendClient()
    private val tts = Tts(app) { ukAvailable ->
        if (!ukAvailable) {
            _ui.value = _ui.value.copy(
                ttsWarning = "Українського голосу TTS немає — відповідь показано текстом.",
            )
        }
    }

    private val _ui = MutableStateFlow(VoiceUi())
    val ui: StateFlow<VoiceUi> = _ui.asStateFlow()

    /** Виклик, коли користувач натиснув кнопку (permission вже надано). */
    fun onPressStart() {
        if (_ui.value.state == UiState.RECORDING) return
        try {
            if (Config.USE_BLUETOOTH_MIC) sco.start()
            recorder.start()
            _ui.value = _ui.value.copy(state = UiState.RECORDING, status = "Слухаю…")
        } catch (e: Exception) {
            fail("Не вдалося почати запис: ${e.message}")
        }
    }

    /** Виклик, коли користувач відпустив кнопку. */
    fun onPressEnd() {
        if (_ui.value.state != UiState.RECORDING) return
        val wav = try {
            recorder.stopAndGetWav()
        } catch (e: Exception) {
            fail("Помилка запису: ${e.message}")
            return
        } finally {
            if (Config.USE_BLUETOOTH_MIC) sco.stop()
        }

        _ui.value = _ui.value.copy(state = UiState.PROCESSING, status = "Обробляю…")
        viewModelScope.launch {
            try {
                val res = backend.sendCommand(wav)
                _ui.value = _ui.value.copy(
                    recognizedText = res.recognizedText,
                    replyText = res.replyText,
                    state = UiState.SPEAKING,
                    status = "Відповідаю…",
                )
                tts.speak(res.replyText)
                delay(1500) // орієнтовно даємо озвучити; для MVP достатньо
                _ui.value = _ui.value.copy(
                    state = UiState.IDLE,
                    status = "Готово. Натисни й утримуй, щоб говорити",
                )
            } catch (e: Exception) {
                fail("Помилка мережі: ${e.message}")
            }
        }
    }

    private fun fail(message: String) {
        tts.speak("Сталася помилка. Спробуй ще раз.")
        _ui.value = _ui.value.copy(state = UiState.ERROR, status = message)
    }

    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }
}
