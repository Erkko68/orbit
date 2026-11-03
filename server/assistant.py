"""The AI assistant, without the HTTP server around it so it can be tested with a fake model."""

import logging
from collections.abc import Callable
from pathlib import Path

FIRST_QUESTION_PROMPT = (Path(__file__).parent / "prompts" / "first_question.md").read_text(encoding="utf-8")

# How many questions an interview may ask. #74 makes it the real budget.
QUESTION_BUDGET = 8

# What the model writes, as JSON Schema. The server adds the id, the position and the budget, which are not the
# model's to decide.
QUESTION_SCHEMA = {
    "type": "object",
    "properties": {
        "text": {"type": "string"},
        "options": {"type": "array", "items": {"type": "string"}},
        "multiSelect": {"type": "boolean"},
        "freeText": {"type": "boolean"},
    },
    "required": ["text", "options", "multiSelect", "freeText"],
    "additionalProperties": False,
}

# The model's answer to a prompt, as JSON that follows a schema.
Generate = Callable[[str, dict], object]


class AssistantError(Exception):
    """Why the assistant did not answer, with a callable error status: UNAUTHENTICATED or UNAVAILABLE."""

    def __init__(self, status: str, message: str, details: dict | None = None):
        super().__init__(message)
        self.status = status
        self.message = message
        self.details = details


def parse_question(answer: object, position: int) -> dict:
    """The question in the model's [answer], in the shape the app parses (#64). Raises ValueError if it is not one."""
    if not isinstance(answer, dict):
        raise ValueError("the answer is not an object")
    text = answer.get("text")
    if not isinstance(text, str) or not text.strip():
        raise ValueError("the question has no text")
    options = answer.get("options")
    if not isinstance(options, list) or not all(isinstance(o, str) and o.strip() for o in options):
        raise ValueError("the options are not a list of texts")
    multi_select = answer.get("multiSelect")
    free_text = answer.get("freeText")
    if not isinstance(multi_select, bool) or not isinstance(free_text, bool):
        raise ValueError("multiSelect and freeText must be true or false")
    if not options and not free_text:
        raise ValueError("the question has no way to answer it")
    return {
        "id": f"q{position}",
        "text": text.strip(),
        "options": [o.strip() for o in options],
        "multiSelect": multi_select,
        "freeText": free_text,
        "position": position,
        "budget": QUESTION_BUDGET,
    }


def first_question(uid: str | None, generate: Generate) -> dict:
    """The interview's first question for the signed-in caller [uid]; a rejected call never reaches the AI. #66 turns this into the whole interview."""
    if uid is None:
        raise AssistantError("UNAUTHENTICATED", "Sign in to use the assistant.")
    try:
        return parse_question(generate(FIRST_QUESTION_PROMPT, QUESTION_SCHEMA), position=1)
    except Exception as e:
        logging.exception("The assistant could not ask its first question")
        raise AssistantError("UNAVAILABLE", "The assistant could not answer.", details={"reason": "unavailable"}) from e
