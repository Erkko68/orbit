"""The model the assistant asks, as a Generate. Which Gemma is configuration (ASSISTANT_MODEL), so #70 can switch without code."""

import json
from collections.abc import Mapping

from assistant.interview import Generate
from assistant.net import Post, post_json


def openai_compatible(base_url: str, model: str, reasoning_effort: str = "none", post: Post = post_json) -> Generate:
    """
    A model behind an OpenAI-compatible chat endpoint, such as Gemma 4 on Ollama next to the server. [reasoning_effort]
    is how long it thinks before answering: "none" turns thinking off, so a question comes back in seconds.
    """

    def generate(prompt: str, schema: dict) -> object:
        reply = post(
            f"{base_url.rstrip('/')}/chat/completions",
            {
                "model": model,
                "messages": [{"role": "user", "content": prompt}],
                "reasoning_effort": reasoning_effort,
                "response_format": {"type": "json_schema", "json_schema": {"name": "answer", "schema": schema}},
            },
        )
        return json.loads(reply["choices"][0]["message"]["content"])

    return generate


def from_env(env: Mapping[str, str]) -> Generate:
    """Gemma on the Ollama next to the server: MODEL_URL and ASSISTANT_MODEL name them, ASSISTANT_REASONING_EFFORT how long it thinks."""
    return openai_compatible(
        env.get("MODEL_URL", "http://ollama:11434/v1"),
        env.get("ASSISTANT_MODEL", "gemma4:e2b"),
        env.get("ASSISTANT_REASONING_EFFORT", "none"),
    )
