"""The models the assistant can ask, each as a Generate. Which one is configuration, so #70 can switch without code."""

import json
from collections.abc import Mapping

from assistant.interview import Generate
from assistant.net import Post, post_json

GEMINI = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"


def openai_compatible(base_url: str, model: str, post: Post = post_json) -> Generate:
    """A model behind an OpenAI-compatible chat endpoint, such as Gemma 4 on Ollama next to the server."""

    def generate(prompt: str, schema: dict) -> object:
        reply = post(
            f"{base_url.rstrip('/')}/chat/completions",
            {
                "model": model,
                "messages": [{"role": "user", "content": prompt}],
                "response_format": {"type": "json_schema", "json_schema": {"name": "answer", "schema": schema}},
            },
        )
        return json.loads(reply["choices"][0]["message"]["content"])

    return generate


def gemini(api_key: str, model: str, post: Post = post_json) -> Generate:
    """Gemini through its REST API, with the key from the server's environment."""

    def generate(prompt: str, schema: dict) -> object:
        reply = post(
            GEMINI.format(model=model),
            {
                "contents": [{"parts": [{"text": prompt}]}],
                "generationConfig": {"responseMimeType": "application/json", "responseJsonSchema": schema},
            },
            headers={"X-Goog-Api-Key": api_key},
        )
        return json.loads(reply["candidates"][0]["content"]["parts"][0]["text"])

    return generate


def from_env(env: Mapping[str, str]) -> Generate:
    """The model [env] names: ASSISTANT_PROVIDER is `local` (the default, Ollama) or `gemini` (needs GEMINI_API_KEY)."""
    provider = env.get("ASSISTANT_PROVIDER", "local")
    if provider == "local":
        return openai_compatible(env.get("MODEL_URL", "http://ollama:11434/v1"), env.get("ASSISTANT_MODEL", "gemma4"))
    if provider == "gemini":
        return gemini(env["GEMINI_API_KEY"], env.get("ASSISTANT_MODEL", "gemini-3.5-flash"))
    raise ValueError(f"Unknown ASSISTANT_PROVIDER {provider!r}: use local or gemini")
