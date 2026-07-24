"""Читання конфігурації з .env (config -- налаштування).

Ключі читаємо ліниво (lazy -- на вимогу), щоб /health працював навіть без
жодного ключа. Валідацію робимо лише там, де ключ реально потрібен.
"""
from __future__ import annotations

import os

from dotenv import load_dotenv

load_dotenv()  # підвантажує змінні з файлу .env, якщо він є


def _get(name: str, default: str | None = None) -> str | None:
    val = os.getenv(name, default)
    if val is not None:
        val = val.strip()
    return val or None


# Необов'язкові при старті (перевіряємо в момент використання)
ANTHROPIC_API_KEY = _get("ANTHROPIC_API_KEY")
OPENAI_API_KEY = _get("OPENAI_API_KEY")
TODOIST_TOKEN = _get("TODOIST_TOKEN")
APP_SHARED_TOKEN = _get("APP_SHARED_TOKEN")

# Зі значеннями за замовчуванням
CLAUDE_MODEL = _get("CLAUDE_MODEL", "claude-haiku-4-5-20251001")
STT_LANGUAGE = _get("STT_LANGUAGE", "uk")


def require(name: str) -> str:
    """Повертає значення ключа або кидає зрозумілу помилку, якщо його немає."""
    val = _get(name)
    if not val:
        raise RuntimeError(
            f"Не задано {name}. Скопіюй backend/.env.example у backend/.env "
            f"і впиши значення."
        )
    return val
