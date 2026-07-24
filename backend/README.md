# backend/ — Python-бекенд голосового асистента

FastAPI-сервіс: приймає аудіо з Android-клієнта, робить STT (Whisper) →
Claude (tool use) → Todoist, повертає текст для озвучування.

## Запуск локально

```bash
cd backend
python -m venv .venv
source .venv/bin/activate            # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env                 # впиши ключі: Anthropic, OpenAI, Todoist
uvicorn app.main:app --reload
```

Сервер підніметься на `http://localhost:8000`.

## Перевірка

```bash
# 1) Жива перевірка (без ключів)
curl http://localhost:8000/health
# -> {"status":"ok"}

# 2) Тестовий WAV 8 кГц (тиша) для перевірки маршруту
python scripts/make_test_wav.py           # створить test_8k.wav
curl -X POST http://localhost:8000/command \
  -H "X-App-Token: $(grep APP_SHARED_TOKEN .env | cut -d= -f2)" \
  -F "audio=@test_8k.wav"
# На тиші STT поверне порожнє -> reply_text="Не почув, повтори будь ласка."

# 3) Реальна команда: запиши коротке аудіо українською як WAV 8кГц моно
#    (напр. «додай задачу купити каву») і надішли так само.
#    -> задача з'явиться в Todoist, reply_text = підтвердження.
```

## Ендпоінти

| Метод | Шлях | Опис |
|---|---|---|
| GET | `/health` | перевірка життєздатності |
| POST | `/command` | audio (multipart) + `X-App-Token` → JSON |

Формат відповіді `/command` — див. `../plan.md` §2.

## Структура

```
app/
  main.py         # маршрути, склейка конвеєра, обробка помилок
  config.py       # ключі з .env (ліниво)
  stt.py          # Whisper (абстракція Transcriber)
  agent.py        # цикл tool use (Claude)
  tools/
    __init__.py   # реєстр інструментів
    todoist.py    # create_task + tool schema
scripts/
  make_test_wav.py
```

## Деплой (коротко)

Python-native хостинг (Supabase/Netlify Functions — це JS, не підходять):
- **Railway / Render** — git-деплой, HTTPS з коробки. Стартова команда:
  `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
- Локальна розробка з телефоном: `uvicorn` + `ngrok http 8000` (тимчасовий HTTPS-URL).

Не забудь задати змінні оточення (ключі) у панелі хостингу.
