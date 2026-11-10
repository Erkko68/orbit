import pytest

from assistant import providers

SCHEMA = {"type": "object"}


def recording(reply):
    """A post that records its calls and answers with [reply]."""
    calls = []

    def post(url, body, headers=None):
        calls.append((url, body, headers))
        return reply

    return post, calls


def test_asks_an_openai_compatible_model_for_json_that_follows_the_schema():
    post, calls = recording({"choices": [{"message": {"content": '{"text": "Who lives there?"}'}}]})

    answer = providers.openai_compatible("http://ollama:11434/v1/", "gemma4", post)("Ask something", SCHEMA)

    assert answer == {"text": "Who lives there?"}
    url, body, _ = calls[0]
    assert url == "http://ollama:11434/v1/chat/completions"
    assert body["model"] == "gemma4"
    assert body["messages"] == [{"role": "user", "content": "Ask something"}]
    assert body["response_format"]["json_schema"]["schema"] == SCHEMA


def test_asks_gemini_for_json_that_follows_the_schema():
    post, calls = recording({"candidates": [{"content": {"parts": [{"text": '{"text": "Hi"}'}]}}]})

    answer = providers.gemini("key", "gemini-3.5-flash", post)("Ask something", SCHEMA)

    assert answer == {"text": "Hi"}
    url, body, headers = calls[0]
    assert url == providers.GEMINI.format(model="gemini-3.5-flash")
    assert body["contents"] == [{"parts": [{"text": "Ask something"}]}]
    assert body["generationConfig"] == {"responseMimeType": "application/json", "responseJsonSchema": SCHEMA}
    assert headers == {"X-Goog-Api-Key": "key"}


def test_the_local_model_is_the_default(monkeypatch):
    made = []
    monkeypatch.setattr(providers, "openai_compatible", lambda url, model: made.append((url, model)))

    providers.from_env({})

    assert made == [("http://ollama:11434/v1", "gemma4:e2b")]


def test_gemini_takes_its_key_from_the_environment(monkeypatch):
    made = []
    monkeypatch.setattr(providers, "gemini", lambda key, model: made.append((key, model)))

    providers.from_env({"ASSISTANT_PROVIDER": "gemini", "GEMINI_API_KEY": "secret"})

    assert made == [("secret", "gemini-3.5-flash")]


def test_an_unknown_provider_is_a_configuration_error():
    with pytest.raises(ValueError):
        providers.from_env({"ASSISTANT_PROVIDER": "other"})
