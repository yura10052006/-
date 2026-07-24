"""STT -- Speech-to-Text (розпізнавання мови -> текст).

Абстракція `Transcriber` дозволяє пізніше підмінити провайдера (локальний
Whisper, Google STT) без правок решти коду -- рішення D-2 у spec.md.
"""
from __future__ import annotations

import io
from typing import Protocol

from . import config


class Transcriber(Protocol):
    """Інтерфейс розпізнавача. Приймає байти аудіо -> повертає текст."""

    def transcribe(self, audio_bytes: bytes, *, filename: str = "audio.wav") -> str: ...


class WhisperTranscriber:
    """Реалізація на OpenAI Whisper API (`whisper-1`).

    Стійкий до низького бітрейту/шуму -- важливо для 8 кГц мікрофона окулярів
    (NFR-2). Мова фіксується (`uk`), щоб не гадати.
    """

    def __init__(self, model: str = "whisper-1", language: str | None = None):
        self.model = model
        self.language = language or config.STT_LANGUAGE
        self._client = None  # створимо ліниво, коли реально треба

    def _get_client(self):
        if self._client is None:
            from openai import OpenAI

            self._client = OpenAI(api_key=config.require("OPENAI_API_KEY"))
        return self._client

    def transcribe(self, audio_bytes: bytes, *, filename: str = "audio.wav") -> str:
        client = self._get_client()
        # OpenAI SDK хоче файлоподібний об'єкт з іменем (щоб визначити формат)
        buf = io.BytesIO(audio_bytes)
        buf.name = filename
        resp = client.audio.transcriptions.create(
            model=self.model,
            file=buf,
            language=self.language,
        )
        return (resp.text or "").strip()


# Єдиний екземпляр за замовчуванням
default_transcriber: Transcriber = WhisperTranscriber()
