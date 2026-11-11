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


def test_the_local_model_is_the_default(monkeypatch):
    made = []
    monkeypatch.setattr(providers, "openai_compatible", lambda url, model: made.append((url, model)))

    providers.from_env({})

    assert made == [("http://ollama:11434/v1", "gemma4:e2b")]

