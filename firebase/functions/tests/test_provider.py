from types import SimpleNamespace

from assistant.provider import GeminiProvider


class FakeModels:
    def __init__(self, text):
        self.text = text
        self.calls = []

    def generate_content(self, **kwargs):
        self.calls.append(kwargs)
        return SimpleNamespace(text=self.text)


def test_asks_the_configured_model_for_json_that_follows_the_schema():
    models = FakeModels('{"text": "Who lives there?"}')
    provider = GeminiProvider(SimpleNamespace(models=models), "a-model")
    schema = {"type": "OBJECT"}

    answer = provider.generate("Ask something", schema)

    assert answer == {"text": "Who lives there?"}
    call = models.calls[0]
    assert call["model"] == "a-model"
    assert call["contents"] == "Ask something"
    assert call["config"].response_mime_type == "application/json"
    assert call["config"].response_schema is not None
