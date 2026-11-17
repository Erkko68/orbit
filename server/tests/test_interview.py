import json
from datetime import date

import pytest

from assistant.interview import (
    QUESTION_BUDGET,
    QUESTION_SCHEMA,
    AssistantError,
    example_dates,
    next_step,
    parse_question,
)
from assistant.proposals import PLAN_SCHEMA, item_errors

SIGNED_IN = "ana"


def valid(**changes):
    answer = {
        "text": " Who lives there? ",
        "options": ["Roommates", " Family "],
        "multiSelect": False,
        "freeText": True,
        "ready": False,
    }
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
        "suggestion": "Roommates",
    }


def test_suggests_the_option_the_model_would_choose():
    assert parse_question(valid(suggestion=" Family "), position=1)["suggestion"] == "Family"


def test_a_suggestion_that_is_not_an_option_falls_back_to_the_first():
    assert parse_question(valid(suggestion="A boat"), position=1)["suggestion"] == "Roommates"


def test_a_question_without_options_suggests_words_or_nothing():
    assert parse_question(valid(options=[], suggestion="Three of us"), position=1)["suggestion"] == "Three of us"
    assert "suggestion" not in parse_question(valid(options=[], suggestion=" "), position=1)


def test_an_answer_the_assistant_chose_says_so():
    model = Model()
    decided = {**ANSWER, "decided": True}

    next_step(SIGNED_IN, interview([decided]), model)

    assert "A: Family, two kids (chosen by the assistant)" in model.prompts[0][0]


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


ANSWER = {"questionId": "q1", "question": "Who lives there?", "selected": ["Family"], "text": "two kids"}
BINS = {"title": "Take out the bins", "modules": {"assignment": {"mode": "member", "assignees": ["m2"]}}}
PLAN = {"version": 1, "space": {"name": "Home", "accent": 2}, "items": [BINS]}


def interview(answers=(), **changes):
    data = {
        "answers": list(answers),
        "language": "es",
        "today": "2026-11-16",
        "newSpace": False,
        "members": [{"name": "Ana"}, {"name": "Ben"}],
        "items": ["Groceries"],
    }
    return {**data, **changes}


class Model:
    """A fake model: the question or the plan it is given, by the schema it is asked for. Records every prompt."""

    def __init__(self, question=None, plan=None):
        self.question = valid() if question is None else question
        self.plan = PLAN if plan is None else plan
        self.prompts = []

    def __call__(self, prompt, schema):
        self.prompts.append((prompt, schema))
        return self.plan if schema is PLAN_SCHEMA else self.question


def test_asks_the_first_question_with_the_space_and_the_language():
    model = Model()

    result = next_step(SIGNED_IN, interview(), model)

    assert result["id"] == "q1"
    assert result["position"] == 1
    [(prompt, schema)] = model.prompts
    assert schema is QUESTION_SCHEMA
    assert "- m1: Ana" in prompt and "- m2: Ben" in prompt
    assert "- Groceries" in prompt
    assert "and the suggestion in Spanish." in prompt
    assert "(nothing asked yet)" in prompt


def test_the_first_question_is_asked_even_if_the_model_says_it_is_ready():
    assert next_step(SIGNED_IN, interview(), Model(question=valid(ready=True)))["id"] == "q1"


def test_asks_the_next_question_with_what_was_answered():
    model = Model()

    result = next_step(SIGNED_IN, interview([ANSWER]), model)

    assert result["id"] == "q2"
    assert "Q: Who lives there?\nA: Family, two kids" in model.prompts[0][0]
    assert f"you may ask at most {QUESTION_BUDGET - 1} more" in model.prompts[0][0]


def test_once_the_model_is_ready_it_proposes_the_checked_plan_for_the_space():
    model = Model(question=valid(ready=True))

    result = next_step(SIGNED_IN, interview([ANSWER]), model)

    # An existing space gets no other space, whatever the model proposed.
    assert result == {"plan": {"version": 1, "items": [BINS]}}
    plan_prompt, schema = model.prompts[1]
    assert schema is PLAN_SCHEMA
    assert "Today is Monday 2026-11-16." in plan_prompt
    assert "- 2026-11-22 Sunday (7)" in plan_prompt
    assert "2026-11-30" not in plan_prompt
    assert "do not propose one" in plan_prompt


def test_at_the_budget_it_proposes_without_asking_again():
    model = Model()

    result = next_step(SIGNED_IN, interview([ANSWER] * QUESTION_BUDGET), model)

    assert "plan" in result
    assert [schema for _, schema in model.prompts] == [PLAN_SCHEMA]


def test_a_new_space_plan_names_the_space():
    model = Model(question=valid(ready=True))

    result = next_step(SIGNED_IN, interview([ANSWER], newSpace=True, members=[{"name": "Ana"}]), model)

    assert result["plan"]["space"] == {"name": "Home", "accent": 2}
    assert "propose the space" in model.prompts[1][0]
    assert "they are its only member" in model.prompts[1][0]
    # Ben is not in a space that does not exist yet: the item for m2 is dropped.
    assert result["plan"]["items"] == []


def test_a_new_space_plan_without_a_space_is_unavailable():
    plan = {"version": 1, "items": [BINS]}

    with pytest.raises(AssistantError) as error:
        next_step(SIGNED_IN, interview([ANSWER], newSpace=True), Model(question=valid(ready=True), plan=plan))

    assert error.value.status == "UNAVAILABLE"


def test_an_unknown_language_is_english():
    model = Model()

    next_step(SIGNED_IN, interview(language="fr"), model)
    next_step(SIGNED_IN, interview(language=["es"]), model)

    assert all("and the suggestion in English." in prompt for prompt, _ in model.prompts)


def test_the_prompt_text_comes_from_the_versioned_prompt_files():
    model = Model()

    next_step(SIGNED_IN, {"answers": []}, model)

    assert "one question at a time" in model.prompts[0][0]
    assert "- (none listed)" in model.prompts[0][0]
    assert "and the suggestion in English." in model.prompts[0][0]


@pytest.mark.parametrize(
    "data",
    [
        None,
        "answers",
        {"answers": "none"},
        {"answers": [ANSWER] * (QUESTION_BUDGET + 1)},
        {"answers": ["not an answer"]},
        {"answers": [{**ANSWER, "question": " "}]},
        {"answers": [{**ANSWER, "selected": "Family"}]},
        {"members": [{"email": "ana@orbit.app"}]},
        {"members": "Ana"},
        {"items": [3]},
        {"today": "tomorrow"},
    ],
)
def test_rejects_what_is_not_an_interview_before_the_model(data):
    calls = []

    with pytest.raises(AssistantError) as error:
        next_step(SIGNED_IN, data, lambda *args: calls.append(args))

    assert error.value.status == "INVALID_ARGUMENT"
    assert calls == []


def test_rejects_an_unauthenticated_call_without_reaching_the_model():
    calls = []

    with pytest.raises(AssistantError) as error:
        next_step(None, interview(), lambda *args: calls.append(args))

    assert error.value.status == "UNAUTHENTICATED"
    assert calls == []


def fail(*args):
    raise RuntimeError("provider down")


@pytest.mark.parametrize(
    "answers, generate",
    [
        ([], fail),
        ([], lambda *args: valid(text="")),
        ([ANSWER], Model(question=valid(ready=True), plan={"version": 2, "items": []})),
    ],
)
def test_a_failed_or_invalid_answer_is_unavailable_and_never_passed_on(answers, generate):
    with pytest.raises(AssistantError) as error:
        next_step(SIGNED_IN, interview(answers), generate)

    assert error.value.status == "UNAVAILABLE"
    assert error.value.details == {"reason": "unavailable"}


@pytest.mark.parametrize(
    ("today", "expected"),
    [
        (date(2026, 11, 16), {"monday": "2026-11-16", "saturday": "2026-11-21", "sunday": "2026-11-22", "first": "2026-12-01"}),
        (date(2026, 11, 22), {"monday": "2026-11-23", "saturday": "2026-11-28", "sunday": "2026-11-29", "first": "2026-12-01"}),
        (date(2026, 12, 31), {"monday": "2027-01-04", "saturday": "2027-01-02", "sunday": "2027-01-03", "first": "2027-01-01"}),
    ],
)
def test_the_examples_dates_are_never_in_the_past(today, expected):
    assert example_dates(today) == expected


def test_every_example_item_in_the_plan_prompt_is_a_valid_proposal():
    model = Model(question=valid(ready=True))
    next_step(SIGNED_IN, interview([ANSWER]), model)
    plan_prompt = model.prompts[1][0]

    examples = [json.loads(line) for line in plan_prompt.splitlines() if line.startswith('{"title"')]

    assert len(examples) == 5
    assert all(item_errors(example, {"m1", "m2", "m3"}) == [] for example in examples)
