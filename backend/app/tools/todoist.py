"""Інструмент create_task -- створення задачі в Todoist (REST API v2).

Це той самий «реальний tool use», який доводить: Claude не просто відповідає,
а робить дію в зовнішньому сервісі (Definition of Done, spec §2).
"""
from __future__ import annotations

import httpx

from .. import config

_API_URL = "https://api.todoist.com/rest/v2/tasks"


def create_task(content: str, due_string: str | None = None) -> dict:
    """Створює задачу в Todoist.

    :param content: текст задачі, напр. «купити каву».
    :param due_string: дата природною мовою, напр. «завтра», «сьогодні о 18:00».
                       Todoist сам це парсить -- дати майже безкоштовні.
    :return: {ok, id, content, url} або {ok: False, error}.
    """
    token = config.require("TODOIST_TOKEN")
    payload: dict[str, str] = {"content": content}
    if due_string:
        payload["due_string"] = due_string

    try:
        resp = httpx.post(
            _API_URL,
            headers={"Authorization": f"Bearer {token}"},
            json=payload,
            timeout=15.0,
        )
        resp.raise_for_status()
        data = resp.json()
        return {
            "ok": True,
            "id": str(data.get("id", "")),
            "content": data.get("content", content),
            "url": data.get("url", ""),
        }
    except httpx.HTTPStatusError as e:
        return {"ok": False, "error": f"Todoist HTTP {e.response.status_code}"}
    except httpx.HTTPError as e:
        return {"ok": False, "error": f"Todoist недоступний: {e.__class__.__name__}"}


# Опис інструмента для Claude (tool schema -- схема інструмента).
# Claude читає це, щоб зрозуміти, коли й з якими аргументами викликати.
CREATE_TASK_SCHEMA = {
    "name": "create_task",
    "description": (
        "Створити задачу (нагадування, справу) у списку задач користувача. "
        "Виклич, коли користувач просить щось додати, нагадати, запланувати."
    ),
    "input_schema": {
        "type": "object",
        "properties": {
            "content": {
                "type": "string",
                "description": "Текст задачі українською, напр. 'купити каву'.",
            },
            "due_string": {
                "type": "string",
                "description": (
                    "Необов'язково. Термін природною мовою: 'сьогодні', "
                    "'завтра', 'завтра о 18:00', 'у понеділок'. Якщо користувач "
                    "часу не назвав -- не передавай."
                ),
            },
        },
        "required": ["content"],
    },
}
