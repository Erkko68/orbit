"""The AI provider, behind an interface so the model can change (#70) without touching the callers."""

import json
from typing import Protocol

from google.genai import types


class Provider(Protocol):
    def generate(self, prompt: str, schema: dict) -> object:
        """The model's answer to [prompt], as JSON that follows [schema]."""
        ...


class GeminiProvider:
    """Gemini on Vertex AI. The client authenticates as the function's service account, so there is no key."""

    def __init__(self, client, model: str):
        self._client = client
        self._model = model

    def generate(self, prompt: str, schema: dict) -> object:
        response = self._client.models.generate_content(
            model=self._model,
            contents=prompt,
            config=types.GenerateContentConfig(response_mime_type="application/json", response_schema=schema),
        )
        return json.loads(response.text)
