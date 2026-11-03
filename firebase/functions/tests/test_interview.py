import pytest
from firebase_functions.https_fn import FunctionsErrorCode, HttpsError

from assistant.interview import FIRST_QUESTION_PROMPT, first_question
from assistant.question import MODEL_SCHEMA

SIGNED_IN = object()


class FakeProvider:
    """Answers with [answer], or raises it when it is an exception. Records what it was asked."""

    def __init__(self, answer):
        self.answer = answer
        self.calls = []

    def generate(self, prompt, schema):
        self.calls.append((prompt, schema))
        if isinstance(self.answer, Exception):
            raise self.answer
        return self.answer


QUESTION = {"text": "Who lives there?", "options": ["Roommates", "Family"], "multiSelect": False, "freeText": True}


def test_returns_the_first_question_to_a_signed_in_caller():
    provider = FakeProvider(QUESTION)

    result = first_question(SIGNED_IN, lambda: provider)

    assert result["id"] == "q1"
    assert result["text"] == "Who lives there?"
    assert result["position"] == 1
    assert provider.calls == [(FIRST_QUESTION_PROMPT, MODEL_SCHEMA)]


def test_rejects_an_unauthenticated_call_without_reaching_the_provider():
    made = []

    with pytest.raises(HttpsError) as error:
        first_question(None, lambda: made.append(True))

    assert error.value.code == FunctionsErrorCode.UNAUTHENTICATED
    assert made == []


@pytest.mark.parametrize(
    "answer",
    [RuntimeError("provider down"), {"text": "", "options": [], "multiSelect": False, "freeText": True}],
)
def test_a_failed_or_invalid_answer_is_unavailable_and_never_passed_on(answer):
    with pytest.raises(HttpsError) as error:
        first_question(SIGNED_IN, lambda: FakeProvider(answer))

    assert error.value.code == FunctionsErrorCode.UNAVAILABLE
    assert error.value.details == {"reason": "unavailable"}


def test_the_prompt_is_a_file():
    assert "first question" in FIRST_QUESTION_PROMPT
