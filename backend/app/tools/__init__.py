"""Реєстр інструментів (tools) для агента.

Щоб додати новий інструмент: напиши функцію + схему, і додай сюди в TOOLS
та TOOL_SCHEMAS. Агент (agent.py) підхопить автоматично.
"""
from __future__ import annotations

from typing import Any, Callable

from . import todoist

# Ім'я інструмента -> функція-виконавець (Python)
TOOLS: dict[str, Callable[..., dict]] = {
    "create_task": todoist.create_task,
}

# Список схем для передачі Claude
TOOL_SCHEMAS: list[dict[str, Any]] = [
    todoist.CREATE_TASK_SCHEMA,
]


def run_tool(name: str, tool_input: dict) -> dict:
    """Виконати інструмент за іменем. Невідомий інструмент -> помилка (не краш)."""
    fn = TOOLS.get(name)
    if fn is None:
        return {"ok": False, "error": f"Невідомий інструмент: {name}"}
    return fn(**tool_input)
