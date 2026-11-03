"""The interview callable, without the Firebase wiring so it can be tested with a fake provider."""

import logging
from collections.abc import Callable
from pathlib import Path

from firebase_functions.https_fn import FunctionsErrorCode, HttpsError

from assistant.provider import Provider
from assistant.question import MODEL_SCHEMA, parse_question

FIRST_QUESTION_PROMPT = (Path(__file__).parent.parent / "prompts" / "first_question.md").read_text(encoding="utf-8")


def first_question(auth: object | None, provider: Callable[[], Provider]) -> dict:
    """
    The interview's first question for a signed-in caller. [provider] is only made once the caller is known,
    so a rejected call never reaches the AI. #66 turns this into the whole interview.
    """
    if auth is None:
        raise HttpsError(FunctionsErrorCode.UNAUTHENTICATED, "Sign in to use the assistant.")
    try:
        answer = provider().generate(FIRST_QUESTION_PROMPT, MODEL_SCHEMA)
        question = parse_question(answer, position=1)
    except Exception as e:
        logging.exception("The assistant could not ask its first question")
        raise HttpsError(
            FunctionsErrorCode.UNAVAILABLE,
            "The assistant could not answer.",
            details={"reason": "unavailable"},
        ) from e
    return question.to_dict()
