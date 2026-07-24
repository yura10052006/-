package com.voiceassistant.net

import com.voiceassistant.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Результат виклику /command, розібраний з JSON. */
data class CommandResult(
    val recognizedText: String,
    val replyText: String,
    val error: String?,
)

/**
 * Тонкий HTTP-клієнт: один multipart-POST аудіо на бекенд і розбір відповіді.
 * Уся «розумна» логіка — на бекенді; тут лише транспорт.
 */
class BackendClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // STT+LLM можуть зайняти кілька секунд
        .build()

    /** Надсилає WAV на /command. Викликати з корутини (не в UI-потоці). */
    suspend fun sendCommand(wav: ByteArray): CommandResult = withContext(Dispatchers.IO) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "audio",
                "command.wav",
                wav.toRequestBody("audio/wav".toMediaType()),
            )
            .build()

        val request = Request.Builder()
            .url(Config.BASE_URL.trimEnd('/') + "/command")
            .header("X-App-Token", Config.APP_SHARED_TOKEN)
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            if (resp.code == 403) {
                return@withContext CommandResult("", "Невірний токен доступу.", "403")
            }
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful || text.isBlank()) {
                return@withContext CommandResult(
                    "", "Немає зв'язку з сервером.", "http ${resp.code}",
                )
            }
            val json = JSONObject(text)
            CommandResult(
                recognizedText = json.optString("recognized_text", ""),
                replyText = json.optString("reply_text", "Готово."),
                error = json.optString("error", null).takeIf { it != "null" },
            )
        }
    }
}
