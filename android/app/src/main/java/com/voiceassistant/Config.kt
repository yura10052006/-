package com.voiceassistant

/**
 * Налаштування клієнта. Для MVP тримаємо тут; у «дорослій» версії винесли б у
 * BuildConfig / local.properties.
 *
 * ВАЖЛИВО: заміни BASE_URL на URL свого бекенда:
 *  - локальна розробка через ngrok: "https://xxxx.ngrok-free.app"
 *  - або хостинг (Railway/Render): "https://твій-сервіс.up.railway.app"
 *  - або локальний Wi-Fi: "http://192.168.x.x:8000" (тоді дозволь cleartext, див. нижче)
 *
 * APP_SHARED_TOKEN має збігатися зі значенням у backend/.env.
 */
object Config {
    const val BASE_URL: String = "https://CHANGE-ME.ngrok-free.app"
    const val APP_SHARED_TOKEN: String = "changeme-put-a-long-random-string-here"

    /** true -> писати з мікрофона Bluetooth-гарнітури (HFP/SCO).
     *  false -> писати з мікрофона телефона (fallback, коли навушників ще нема). */
    const val USE_BLUETOOTH_MIC: Boolean = false
}
