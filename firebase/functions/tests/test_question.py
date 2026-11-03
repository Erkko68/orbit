import pytest

from assistant.question import QUESTION_BUDGET, InvalidQuestion, parse_question


def valid(**changes):
    answer = {"text": " Who lives there? ", "options": ["Roommates", " Family "], "multiSelect": False, "freeText": True}
    return {**answer, **changes}


def test_parses_a_question_and_adds_what_the_model_does_not_decide():
    question = parse_question(valid(), position=3)

    assert question.to_dict() == {
        "id": "q3",
        "text": "Who lives there?",
        "options": ["Roommates", "Family"],
        "multiSelect": False,
        "freeText": True,
        "position": 3,
        "budget": QUESTION_BUDGET,
    }


def test_a_free_text_question_needs_no_options():
    assert parse_question(valid(options=[]), position=1).options == []


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
    with pytest.raises(InvalidQuestion):
        parse_question(answer, position=1)
