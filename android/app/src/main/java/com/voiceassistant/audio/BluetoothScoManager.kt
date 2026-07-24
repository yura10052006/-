package com.voiceassistant.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

/**
 * Керує каналом SCO (HFP) — щоб писати саме з мікрофона Bluetooth-гарнітури,
 * а не телефона. Це найтонше місце інтеграції (plan §4.4).
 *
 * Послідовність у сценарії: start() -> запис -> stop() -> (пристрій сам
 * перемикається на A2DP) -> озвучення TTS у якісний вихід.
 *
 * На Android 12+ (наш Redmi) використовуємо setCommunicationDevice; на старіших —
 * застарілий startBluetoothSco.
 */
class BluetoothScoManager(context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    /** Спрямувати вхід/вихід на Bluetooth-гарнітуру (SCO). */
    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val sco = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }
            if (sco != null) {
                audioManager.setCommunicationDevice(sco)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.apply {
                mode = AudioManager.MODE_IN_COMMUNICATION
                startBluetoothSco()
                isBluetoothScoOn = true
            }
        }
    }

    /** Повернути маршрутизацію до типової (щоб TTS звучав через A2DP). */
    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            audioManager.apply {
                isBluetoothScoOn = false
                stopBluetoothSco()
                mode = AudioManager.MODE_NORMAL
            }
        }
    }
}
