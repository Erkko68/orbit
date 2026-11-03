from types import SimpleNamespace

import main


class FakeModels:
    def __init__(self):
        self.calls = []

    def generate_content(self, **kwargs):
        self.calls.append(kwargs)
        return SimpleNamespace(text='{"text": "Who lives there?"}')


def test_asks_one_vertex_client_for_json_that_follows_the_schema(monkeypatch):
    made, models = [], FakeModels()
    monkeypatch.setenv("GCLOUD_PROJECT", "orbit-test")
    monkeypatch.setenv("GOOGLE_CLOUD_PROJECT", "orbit-test")
    monkeypatch.setattr("google.genai.Client", lambda **kwargs: made.append(kwargs) or SimpleNamespace(models=models))
    main._client.cache_clear()

    first = main._gemini("Ask something", {"type": "OBJECT"})
    main._gemini("Ask again", {"type": "OBJECT"})

    assert first == {"text": "Who lives there?"}
    assert len(made) == 1
    assert made[0]["vertexai"] is True
    assert made[0]["location"] == "global"
    call = models.calls[0]
    assert call["model"] == "gemini-3.5-flash"
    assert call["contents"] == "Ask something"
    assert call["config"].response_mime_type == "application/json"
    assert call["config"].response_schema is not None
