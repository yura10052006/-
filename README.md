# Голосовий AI-асистент (Claude) — Трек A

Прототип для перевірки гіпотези: **чи зручно давати задачі голосом** — перш ніж купувати
окуляри Ray-Ban Meta. Пайплайн: голос → STT → Claude (з tool use) → TTS → голос у вухо.
Українська мова, звичайні Bluetooth-навушники (окуляри пізніше підключаться так само).

Повний контекст і рішення — у документах SDD:
- **[`spec.md`](spec.md)** — що і навіщо (гіпотеза, межі, критерії).
- **[`plan.md`](plan.md)** — як (архітектура, контракт, стек).
- **[`tasks.md`](tasks.md)** — конкретні задачі з критеріями готовності.

## Архітектура (стисло)

```
Android (тонкий клієнт)  ──WAV 8кГц──►  Python-бекенд (FastAPI)  ──►  Whisper · Claude · Todoist
   кнопка · мікрофон · TTS   ◄──JSON──      STT → tool use → задача
```

- **`backend/`** — Python (FastAPI). Уся логіка: STT (Whisper) → Claude (tool use) → Todoist.
- **`android/`** — тонкий нативний клієнт (Kotlin): push-to-talk, запис з Bluetooth-мікрофона (HFP), озвучування (системний TTS). З'явиться на віхі M4.
- **`legacy/`** — старий непов'язаний каркас, збережений навмисно (див. `legacy/README.md`).

## Швидкий старт (бекенд)

```bash
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # впиши свої ключі: Anthropic, OpenAI, Todoist
uvicorn app.main:app --reload
```

Перевірка: `curl http://localhost:8000/health` → `{"status":"ok"}`.
Деталі — у `backend/README.md`. Android — відкрити теку `android/` в Android Studio.
