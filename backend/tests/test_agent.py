"""Тести циклу tool use в agent.py з підробленим (mock) Anthropic-клієнтом.

Перевіряємо найтоншу логіку — цикл «запит -> tool_use -> tool_result -> фінал» —
без жодного реального виклику API.
"""
from __future__ import annotations

from types import SimpleNamespace

import anthropic

from app import agent, tools


def _text_block(text):
    return SimpleNamespace(type="text", text=text)


def _tool_block(name, tool_input, id="t1"):
    return SimpleNamespace(type="tool_use", name=name, input=tool_input, id=id)


class _FakeMessages:
    def __init__(self, responses):
        self._responses = list(responses)
        self.calls = []

    def create(self, **kwargs):
        self.calls.append(kwargs)
        return self._responses.pop(0)


class _FakeClient:
    def __init__(self, responses):
        self.messages = _FakeMessages(responses)


def _install_fake_client(monkeypatch, responses):
    monkeypatch.setenv("ANTHROPIC_API_KEY", "fake")
    client = _FakeClient(responses)
    monkeypatch.setattr(anthropic, "Anthropic", lambda api_key=None: client)
    return client


def test_agent_calls_tool_then_confirms(monkeypatch):
    # Claude спершу просить інструмент, потім дає текст-підтвердження
    responses = [
        SimpleNamespace(
            stop_reason="tool_use",
            content=[_tool_block("create_task", {"content": "купити каву"})],
        ),
        SimpleNamespace(
            stop_reason="end_turn",
            content=[_text_block("Додав задачу: купити каву")],
        ),
    ]
    _install_fake_client(monkeypatch, responses)

    # Інструмент теж підроблюємо -- без Todoist/мережі
    monkeypatch.setattr(
        tools, "run_tool",
        lambda name, tool_input: {"ok": True, "id": "1", "content": tool_input["content"]},
    )

    result = agent.run_agent("додай задачу купити каву")

    assert result.reply_text == "Додав задачу: купити каву"
    assert result.action is not None
    assert result.action["type"] == "create_task"
    assert result.action["ok"] is True


def test_agent_plain_answer_no_tool(monkeypatch):
    # Просте питання -> без інструмента, одразу текст
    responses = [
        SimpleNamespace(stop_reason="end_turn", content=[_text_block("Зараз 5 година.")]),
    ]
    _install_fake_client(monkeypatch, responses)

    result = agent.run_agent("котра година")
    assert result.reply_text == "Зараз 5 година."
    assert result.action is None


def test_agent_stops_at_turn_limit(monkeypatch):
    # Claude нескінченно просить інструмент -> запобіжник _MAX_TURNS має спрацювати
    def endless_tool_response():
        return SimpleNamespace(
            stop_reason="tool_use",
            content=[_tool_block("create_task", {"content": "x"})],
        )

    responses = [endless_tool_response() for _ in range(agent._MAX_TURNS + 2)]
    _install_fake_client(monkeypatch, responses)
    monkeypatch.setattr(tools, "run_tool", lambda name, tool_input: {"ok": True, "content": "x"})

    result = agent.run_agent("зациклись")
    # Не впало, повернуло щось осмислене
    assert isinstance(result.reply_text, str) and result.reply_text
