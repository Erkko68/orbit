import json
from types import SimpleNamespace

import pytest

import models

SCHEMA = {"type": "object"}


def test_asks_an_openai_compatible_model_for_json_that_follows_the_schema():
    calls = []

    def post(url, body):
        calls.append((url, body))
        return {"choices": [{"message": {"content": '{"text": "Who lives there?"}'}}]}

    answer = models.openai_compatible("http://ollama:11434/v1/", "gemma4", post)("Ask something", SCHEMA)

    assert answer == {"text": "Who lives there?"}
    url, body = calls[0]
    assert url == "http://ollama:11434/v1/chat/completions"
    assert body["model"] == "gemma4"
    assert body["messages"] == [{"role": "user", "content": "Ask something"}]
    assert body["response_format"]["json_schema"]["schema"] == SCHEMA


def test_asks_gemini_for_json_that_follows_the_schema():
    calls = []
    client = SimpleNamespace(
        models=SimpleNamespace(
            generate_content=lambda **kwargs: calls.append(kwargs) or SimpleNamespace(text='{"text": "Hi"}')
        )
    )

    answer = models.gemini(client, "gemini-3.5-flash")("Ask something", SCHEMA)

    assert answer == {"text": "Hi"}
    assert calls[0]["model"] == "gemini-3.5-flash"
    assert calls[0]["contents"] == "Ask something"
    assert calls[0]["config"].response_mime_type == "application/json"
    assert calls[0]["config"].response_json_schema == SCHEMA


def test_posts_json_and_reads_the_json_reply(monkeypatch):
    sent = []

    class Reply:
        def __enter__(self):
            return self

        def __exit__(self, *args):
            return False

        def read(self, *args):
            return b'{"ok": true}'

    def urlopen(request, timeout):
        sent.append((request.full_url, json.loads(request.data), request.get_method()))
        return Reply()

    monkeypatch.setattr("urllib.request.urlopen", urlopen)

    assert models.post_json("http://model/x", {"a": 1}) == {"ok": True}
    assert sent == [("http://model/x", {"a": 1}, "POST")]


def test_the_local_model_is_the_default(monkeypatch):
    made = []
    monkeypatch.setattr(models, "openai_compatible", lambda url, model: made.append((url, model)))

    models.from_env({})

    assert made == [("http://ollama:11434/v1", "gemma4")]


def test_gemini_takes_its_key_from_the_environment(monkeypatch):
    made = []
    monkeypatch.setattr("google.genai.Client", lambda **kwargs: made.append(kwargs) or "client")

    models.from_env({"ASSISTANT_PROVIDER": "gemini", "GEMINI_API_KEY": "secret"})

    assert made == [{"api_key": "secret"}]


def test_an_unknown_provider_is_a_configuration_error():
    with pytest.raises(ValueError):
        models.from_env({"ASSISTANT_PROVIDER": "other"})
