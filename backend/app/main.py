"""FastAPI-бекенд: ендпоінти /health і /command.

/command -- єдиний потрібний для MVP: приймає аудіо, повертає розпізнаний
текст + коротку відповідь для озвучування. Уся логіка склеєна тут.

Принцип: клієнт «дурний». Будь-яка помилка все одно повертає озвучуваний
reply_text (200 OK), крім геть фатального (поганий токен -> 403).
"""
from __future__ import annotations

import time

from fastapi import FastAPI, File, Header, UploadFile
from fastapi.responses import JSONResponse

from . import agent, config, stt

app = FastAPI(title="Voice Assistant Backend", version="0.1.0")


@app.get("/health")
def health() -> dict:
    """Проста перевірка, що бекенд живий (без потреби в ключах)."""
    return {"status": "ok"}


def _check_token(x_app_token: str | None) -> bool:
    """Спільний секрет клієнт<->бекенд. Якщо секрет не налаштований -- пускаємо
    (зручно для локальної розробки), у проді -- задай APP_SHARED_TOKEN."""
    expected = config.APP_SHARED_TOKEN
    if not expected:
        return True
    return x_app_token == expected


@app.post("/command")
async def command(
    audio: UploadFile = File(...),
    x_app_token: str | None = Header(default=None),
    locale: str | None = None,
) -> JSONResponse:
    if not _check_token(x_app_token):
        return JSONResponse(status_code=403, content={"error": "invalid app token"})

    t0 = time.perf_counter()
    timings: dict[str, int] = {}

    # 1) STT
    try:
        audio_bytes = await audio.read()
        t_stt = time.perf_counter()
        recognized = stt.default_transcriber.transcribe(
            audio_bytes, filename=audio.filename or "audio.wav"
        )
        timings["stt"] = _ms(t_stt)
    except Exception as e:  # мережа/ключ/формат -- не крешимо
        return _ok_response(
            recognized_text="",
            reply_text="Не вдалося розпізнати аудіо. Спробуй ще раз.",
            action=None,
            error=f"stt: {e.__class__.__name__}: {e}",
            timings=_finalize(timings, t0),
        )

    if not recognized:
        return _ok_response(
            recognized_text="",
            reply_text="Не почув, повтори будь ласка.",
            action=None,
            error=None,
            timings=_finalize(timings, t0),
        )

    # 2) Агент (Claude + tool use)
    try:
        t_llm = time.perf_counter()
        result = agent.run_agent(recognized)
        timings["llm"] = _ms(t_llm)
    except Exception as e:
        return _ok_response(
            recognized_text=recognized,
            reply_text="Виникла помилка на сервері. Спробуй ще раз.",
            action=None,
            error=f"agent: {e.__class__.__name__}: {e}",
            timings=_finalize(timings, t0),
        )

    return _ok_response(
        recognized_text=recognized,
        reply_text=result.reply_text,
        action=result.action,
        error=None,
        timings=_finalize(timings, t0),
    )


# --- допоміжне ---

def _ms(since: float) -> int:
    return int((time.perf_counter() - since) * 1000)


def _finalize(timings: dict[str, int], t0: float) -> dict[str, int]:
    timings["total"] = _ms(t0)
    return timings


def _ok_response(
    *,
    recognized_text: str,
    reply_text: str,
    action: dict | None,
    error: str | None,
    timings: dict[str, int],
) -> JSONResponse:
    return JSONResponse(
        status_code=200,
        content={
            "recognized_text": recognized_text,
            "reply_text": reply_text,
            "action": action,
            "error": error,
            "timings_ms": timings,
        },
    )
