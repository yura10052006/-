# android/ — тонкий клієнт голосового асистента

Нативний Android-застосунок (Kotlin + Jetpack Compose). Робить лише «залізо»:
push-to-talk, запис аудіо (8 кГц), надсилання на бекенд, озвучування відповіді
системним TTS. Уся логіка — на бекенді (`../backend/`).

> ⚠️ **Статус:** код написано, але **не зібрано в цьому середовищі** (немає Android
> SDK). Збірку й запуск робиш ти в Android Studio на телефоні Redmi 13 Pro.
> Це очікувано для мобільної частини.

## Як відкрити й запустити

1. **Android Studio → Open** → обери теку `android/`.
2. Studio сам довантажить Gradle і **згенерує gradle-wrapper** (файл `gradlew`)
   при першій синхронізації. Дай йому «Sync» і, якщо запропонує, «Upgrade/Fix».
3. Відкрий `app/src/main/java/com/voiceassistant/Config.kt` і впиши:
   - `BASE_URL` — URL твого бекенда (ngrok під час розробки, або Railway/Render).
   - `APP_SHARED_TOKEN` — те саме значення, що в `backend/.env`.
   - `USE_BLUETOOTH_MIC` — `false` поки нема навушників (пише з мікрофона телефона),
     `true` — щоб писати з Bluetooth-гарнітури (HFP).
4. Підключи Redmi (USB, режим розробника) → **Run**.
5. На першому запуску дай дозвіл на **мікрофон** (і Bluetooth, якщо `USE_BLUETOOTH_MIC=true`).
   На MIUI/HyperOS дозволи можуть ховатись глибше — перевір у налаштуваннях застосунку.

## Як користуватись

Тримай велику кнопку → говори українською («додай задачу купити каву») → відпусти.
Через кілька секунд почуєш підтвердження, а задача з'явиться в Todoist.
Колір кнопки = стан: синій (готовий) → червоний (пишу) → жовтий (думаю) → зелений (говорю).

## Мережа (важливо)

- **ngrok / Railway / Render** дають **HTTPS** — працює без додаткових налаштувань.
- Якщо ходиш на **локальний IP по http** (`http://192.168.x.x:8000`), Android блокує
  cleartext-трафік. Тоді або став ngrok, або додай `network_security_config` з дозволом
  cleartext для свого IP (скажу як, коли дійдемо — для MVP простіше ngrok).

## Структура

```
app/src/main/java/com/voiceassistant/
  MainActivity.kt          # хост Compose + дозволи
  Config.kt                # BASE_URL, токен, USE_BLUETOOTH_MIC
  audio/
    AudioRecorder.kt       # AudioRecord -> WAV 8кГц моно
    BluetoothScoManager.kt # HFP-мікрофон (SCO) <-> A2DP
    Tts.kt                 # системний TTS (uk-UA)
  net/
    BackendClient.kt       # OkHttp: POST /command
  ui/
    VoiceViewModel.kt      # стейт-машина
    VoiceScreen.kt         # кнопка + індикація
```

## Відомі місця, що потребують перевірки «в залізі»
- HFP-запис саме з мікрофона гарнітури на MIUI (plan §4.4, ризик R-1).
- Наявність українського голосу в системному TTS (plan §4.5, ризик R-2) —
  застосунок попередить на екрані, якщо голосу немає.
- Перемикання SCO↔A2DP при `USE_BLUETOOTH_MIC=true` (ризик R-6).
