"""Тести ендпоінта /command із підробленими STT та агентом (без ключів/мережі)."""
from __future__ import annotations

import importlib
import struct
import wave
from io import BytesIO

from fastapi.testclient import TestClient


def _tiny_wav() -> bytes:
    buf = BytesIO()
    with wave.open(buf, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(8000)
        w.writeframes(struct.pack("<8000h", *([0] * 8000)))
    return buf.getvalue()


def _make_client(monkeypatch, *, token=None, recognized="", reply="ok", action=None):
    if token is None:
        monkeypatch.delenv("APP_SHARED_TOKEN", raising=False)
    else:
        monkeypatch.setenv("APP_SHARED_TOKEN", token)

    import app.config as cfg
    import app.main as m
    importlib.reload(cfg)
    importlib.reload(m)

    monkeypatch.setattr(m.stt.default_transcriber, "transcribe", lambda b, **k: recognized)
    monkeypatch.setattr(
        m.agent, "run_agent",
        lambda text: m.agent.AgentResult(reply_text=reply, action=action),
    )
    return TestClient(m.app)


def test_health(monkeypatch):
    client = _make_client(monkeypatch)
    r = client.get("/health")
    assert r.status_code == 200 and r.json() == {"status": "ok"}


def test_command_full_success(monkeypatch):
    client = _make_client(
        monkeypatch,
        recognized="додай задачу купити каву",
        reply="Додав задачу: купити каву",
        action={"type": "create_task", "ok": True},
    )
    r = client.post("/command", files={"audio": ("c.wav", _tiny_wav(), "audio/wav")})
    body = r.json()
    assert r.status_code == 200
    assert body["recognized_text"] == "додай задачу купити каву"
    assert body["reply_text"] == "Додав задачу: купити каву"
    assert body["action"]["type"] == "create_task"
    assert "total" in body["timings_ms"]


def test_command_empty_stt_asks_repeat(monkeypatch):
    client = _make_client(monkeypatch, recognized="")
    r = client.post("/command", files={"audio": ("c.wav", _tiny_wav(), "audio/wav")})
    assert r.status_code == 200
    assert r.json()["reply_text"] == "Не почув, повтори будь ласка."


def test_command_bad_token_403(monkeypatch):
    client = _make_client(monkeypatch, token="secret")
    r = client.post("/command", files={"audio": ("c.wav", _tiny_wav(), "audio/wav")})
    assert r.status_code == 403


def test_command_correct_token_ok(monkeypatch):
    client = _make_client(monkeypatch, token="secret", recognized="привіт", reply="Вітаю")
    r = client.post(
        "/command",
        headers={"X-App-Token": "secret"},
        files={"audio": ("c.wav", _tiny_wav(), "audio/wav")},
    )
    assert r.status_code == 200
    assert r.json()["reply_text"] == "Вітаю"
