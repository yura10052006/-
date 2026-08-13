"""Тести інструмента Todoist із підробленим (mock) HTTP -- без реальних викликів."""
from __future__ import annotations

import httpx
import pytest

from app import config
from app.tools import todoist


class _FakeResponse:
    def __init__(self, status_code=200, data=None):
        self.status_code = status_code
        self._data = data or {}

    def json(self):
        return self._data

    def raise_for_status(self):
        if self.status_code >= 400:
            raise httpx.HTTPStatusError(
                "err", request=httpx.Request("POST", "http://x"), response=self  # type: ignore[arg-type]
            )


def test_create_task_success(monkeypatch):
    monkeypatch.setattr(config, "TODOIST_TOKEN", "fake-token")
    monkeypatch.setenv("TODOIST_TOKEN", "fake-token")

    captured = {}

    def fake_post(url, headers=None, json=None, timeout=None):
        captured["url"] = url
        captured["headers"] = headers
        captured["json"] = json
        return _FakeResponse(200, {"id": 123, "content": "купити каву", "url": "http://td/123"})

    monkeypatch.setattr(httpx, "post", fake_post)

    result = todoist.create_task("купити каву", due_string="завтра")

    assert result["ok"] is True
    assert result["id"] == "123"
    assert result["content"] == "купити каву"
    assert captured["json"] == {"content": "купити каву", "due_string": "завтра"}
    assert "Bearer fake-token" in captured["headers"]["Authorization"]


def test_create_task_no_due(monkeypatch):
    monkeypatch.setenv("TODOIST_TOKEN", "fake-token")

    def fake_post(url, headers=None, json=None, timeout=None):
        # due_string не має передаватись, якщо його немає
        assert "due_string" not in json
        return _FakeResponse(200, {"id": 1, "content": "тест", "url": ""})

    monkeypatch.setattr(httpx, "post", fake_post)
    result = todoist.create_task("тест")
    assert result["ok"] is True


def test_create_task_http_error(monkeypatch):
    monkeypatch.setenv("TODOIST_TOKEN", "fake-token")

    def fake_post(url, headers=None, json=None, timeout=None):
        return _FakeResponse(401)

    monkeypatch.setattr(httpx, "post", fake_post)
    result = todoist.create_task("тест")
    assert result["ok"] is False
    assert "401" in result["error"]
