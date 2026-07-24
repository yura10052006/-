"""Агент: цикл tool use на Anthropic Messages API.

Серце системи. Отримує текст користувача -> вирішує, чи викликати інструмент
(напр. create_task) -> повертає коротку відповідь для озвучування.

Патерн той самий, що у твоєму Instagram-агенті: цикл messages.create доки
Claude не перестане просити інструменти.
"""
from __future__ import annotations

from dataclasses import dataclass

from . import config, tools

_SYSTEM_PROMPT = (
    "Ти — голосовий асистент. Користувач говорить українською, а твоя відповідь "
    "буде ОЗВУЧЕНА вголос у навушник. Тому:\n"
    "- Відповідай КОРОТКО, однією природною фразою українською.\n"
    "- Без списків, розмітки, емодзі, посилань — це звучатиме як мова.\n"
    "- Якщо користувач просить додати задачу, нагадати чи запланувати справу — "
    "виклич інструмент create_task.\n"
    "- Після створення задачі підтверди коротко, напр.: «Додав задачу: купити каву».\n"
    "- Якщо це просто запитання — відповідай стисло по суті."
)

_MAX_TURNS = 4  # запобіжник від нескінченного циклу tool use


@dataclass
class AgentResult:
    reply_text: str
    action: dict | None  # що реально зроблено (напр. результат create_task) або None


def run_agent(user_text: str) -> AgentResult:
    """Проганяє текст користувача через Claude з інструментами."""
    from anthropic import Anthropic

    client = Anthropic(api_key=config.require("ANTHROPIC_API_KEY"))

    messages: list[dict] = [{"role": "user", "content": user_text}]
    last_action: dict | None = None

    for _ in range(_MAX_TURNS):
        resp = client.messages.create(
            model=config.CLAUDE_MODEL,
            max_tokens=400,
            system=_SYSTEM_PROMPT,
            tools=tools.TOOL_SCHEMAS,
            messages=messages,
        )

        if resp.stop_reason == "tool_use":
            # Додаємо відповідь асистента (з блоками tool_use) у діалог
            messages.append({"role": "assistant", "content": resp.content})

            # Виконуємо кожен запитаний інструмент і збираємо результати
            tool_results = []
            for block in resp.content:
                if block.type == "tool_use":
                    result = tools.run_tool(block.name, dict(block.input))
                    last_action = {"type": block.name, **result}
                    tool_results.append(
                        {
                            "type": "tool_result",
                            "tool_use_id": block.id,
                            "content": _stringify(result),
                        }
                    )
            messages.append({"role": "user", "content": tool_results})
            continue  # ще один виток -- нехай Claude сформулює підтвердження

        # Звичайна текстова відповідь -> завершуємо
        return AgentResult(reply_text=_extract_text(resp), action=last_action)

    # Дійшли до ліміту витків -- повертаємо що є
    return AgentResult(
        reply_text="Готово." if last_action else "Не вдалося обробити запит.",
        action=last_action,
    )


def _extract_text(resp) -> str:
    parts = [b.text for b in resp.content if getattr(b, "type", None) == "text"]
    text = " ".join(p.strip() for p in parts if p).strip()
    return text or "Готово."


def _stringify(result: dict) -> str:
    """Короткий текстовий підсумок результату інструмента для Claude."""
    if result.get("ok"):
        return f"Успіх. Задача створена: {result.get('content', '')}".strip()
    return f"Помилка: {result.get('error', 'невідома')}"
