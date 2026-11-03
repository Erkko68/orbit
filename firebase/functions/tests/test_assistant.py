import pytest
from firebase_functions.https_fn import FunctionsErrorCode, HttpsError

from assistant import FIRST_QUESTION_PROMPT, QUESTION_BUDGET, QUESTION_SCHEMA, first_question, parse_question

SIGNED_IN = object()


def valid(**changes):
    answer = {"text": " Who lives there? ", "options": ["Roommates", " Family "], "multiSelect": False, "freeText": True}
    return {**answer, **changes}


def test_parses_a_question_and_adds_what_the_model_does_not_decide():
    assert parse_question(valid(), position=3) == {
        "id": "q3",
        "text": "Who lives there?",
        "options": ["Roommates", "Family"],
        "multiSelect": False,
        "freeText": True,
        "position": 3,
        "budget": QUESTION_BUDGET,
    }


def test_a_free_text_question_needs_no_options():
    assert parse_question(valid(options=[]), position=1)["options"] == []


@pytest.mark.parametrize(
    "answer",
    [
        "not an object",
        valid(text="   "),
        valid(text=None),
        {"options": [], "multiSelect": False, "freeText": True},
        valid(options="Roommates"),
        valid(options=["Roommates", ""]),
        valid(options=["Roommates", 3]),
        valid(multiSelect="no"),
        valid(freeText=None),
        valid(options=[], freeText=False),
    ],
)
def test_rejects_what_is_not_a_question(answer):
    with pytest.raises(ValueError):
        parse_question(answer, position=1)


def test_asks_the_model_the_first_question_for_a_signed_in_caller():
    calls = []

    result = first_question(SIGNED_IN, lambda prompt, schema: calls.append((prompt, schema)) or valid())

    assert result["id"] == "q1"
    assert result["position"] == 1
    assert calls == [(FIRST_QUESTION_PROMPT, QUESTION_SCHEMA)]
    assert "first question" in FIRST_QUESTION_PROMPT


def test_rejects_an_unauthenticated_call_without_reaching_the_model():
    calls = []

    with pytest.raises(HttpsError) as error:
        first_question(None, lambda *args: calls.append(args))

    assert error.value.code == FunctionsErrorCode.UNAUTHENTICATED
    assert calls == []


def fail(*args):
    raise RuntimeError("provider down")


@pytest.mark.parametrize("generate", [fail, lambda *args: valid(text="")])
def test_a_failed_or_invalid_answer_is_unavailable_and_never_passed_on(generate):
    with pytest.raises(HttpsError) as error:
        first_question(SIGNED_IN, generate)

    assert error.value.code == FunctionsErrorCode.UNAVAILABLE
    assert error.value.details == {"reason": "unavailable"}
