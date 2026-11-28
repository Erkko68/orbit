import pytest

from assistant.interview import QUESTION_SCHEMA, AssistantError, next_step
from assistant.removals import (
    MAX_REASON,
    MAX_REMOVABLE,
    REMOVAL_SCHEMA,
    Removable,
    checked_removal,
    parse_removable,
    removable_text,
)

SIGNED_IN = "ana"
REQUEST = {"questionId": "q1", "question": "What do you want to organise?", "selected": [], "text": "Remove the old lists"}
REMOVABLE = [
    {"title": "Groceries", "kind": "list", "lastDone": "2026-11-15"},
    {"title": "Party shopping", "kind": "list", "lastDone": "2026-03-02"},
    {"title": "Water the plants", "kind": "task"},
]
ITEMS = parse_removable(REMOVABLE)


def interview(**changes):
    data = {
        "answers": [REQUEST],
        "language": "es",
        "today": "2026-11-16",
        "newSpace": False,
        "members": [{"name": "Ana"}],
        "items": ["Groceries", "Party shopping", "Water the plants"],
        "removable": REMOVABLE,
    }
    return {**data, **changes}


class Model:
    """A fake model that asks to remove, then proposes [removal]. Records every prompt with its schema."""

    def __init__(self, removal=None, remove=True):
        self.removal = {"items": [{"item": "i2", "reason": " Not used since March "}]} if removal is None else removal
        self.remove = remove
        self.prompts = []

    def __call__(self, prompt, schema):
        self.prompts.append((prompt, schema))
        if schema is REMOVAL_SCHEMA:
            return self.removal
        return {"remove": self.remove, "ready": False, "text": "Which lists?", "options": [], "multiSelect": False,
                "freeText": True, "suggestion": ""}


def test_names_each_removable_item_by_a_handle_in_order():
    assert ITEMS == [
        Removable("i1", "Groceries", "list", "2026-11-15"),
        Removable("i2", "Party shopping", "list", "2026-03-02"),
        Removable("i3", "Water the plants", "task", ""),
    ]


def test_an_unknown_kind_is_a_plain_item_and_long_texts_are_cut():
    [item] = parse_removable([{"title": "x" * 500, "kind": "space", "lastDone": "2026-11-15T10:00:00Z"}])

    assert item.kind == "item"
    assert len(item.title) == 120
    assert item.last_done == "2026-11-15"


@pytest.mark.parametrize(
    "value",
    [
        "Groceries",
        ["Groceries"],
        [{"kind": "list"}],
        [{"title": "  "}],
        [{"title": "Groceries", "lastDone": 3}],
        [{"title": "Groceries"}] * (MAX_REMOVABLE + 1),
    ],
)
def test_rejects_what_is_not_a_list_of_removable_items(value):
    with pytest.raises(ValueError):
        parse_removable(value)


def test_the_model_sees_handles_kinds_and_when_each_was_last_done():
    text = removable_text(ITEMS)

    assert '- i1: "Groceries" (list, last done 2026-11-15)' in text
    assert '- i3: "Water the plants" (task, never done)' in text
    assert "(none" in removable_text([])


def test_keeps_only_the_handles_sent_each_once_with_a_short_reason():
    answer = {
        "items": [
            {"item": " i2 ", "reason": "r" * 300},
            {"item": "i2", "reason": "again"},
            {"item": "i9", "reason": "not sent"},
            {"item": "item-abc", "reason": "an id, not a handle"},
            {"item": "i3"},
            "not an object",
        ],
    }

    assert checked_removal(answer, ITEMS) == {
        "items": [{"item": "i2", "reason": "r" * MAX_REASON}, {"item": "i3", "reason": ""}],
    }


@pytest.mark.parametrize("answer", [None, [], {"items": "i1"}, {}])
def test_rejects_an_answer_that_is_not_a_removal(answer):
    with pytest.raises(ValueError):
        checked_removal(answer, ITEMS)


def test_a_removal_request_proposes_the_checked_items_to_delete():
    model = Model()

    result = next_step(SIGNED_IN, interview(), model)

    assert result == {"removal": {"items": [{"item": "i2", "reason": "Not used since March"}]}}
    [(_, first), (prompt, second)] = model.prompts
    assert first is QUESTION_SCHEMA and second is REMOVAL_SCHEMA
    assert '- i2: "Party shopping" (list, last done 2026-03-02)' in prompt
    assert "Today is Monday 2026-11-16." in prompt
    assert "A: Remove the old lists" in prompt
    assert "# The user's language\n\nSpanish" in prompt


def test_with_nothing_the_user_may_delete_it_proposes_nothing_without_asking_the_model_again():
    model = Model()

    result = next_step(SIGNED_IN, interview(removable=[]), model)

    assert result == {"removal": {"items": []}}
    assert len(model.prompts) == 1


def test_a_new_space_never_gets_a_removal():
    model = Model()

    result = next_step(SIGNED_IN, interview(newSpace=True), model)

    assert result["text"] == "Which lists?"
    assert all(schema is not REMOVAL_SCHEMA for _, schema in model.prompts)


def test_an_unusable_removal_is_unavailable():
    with pytest.raises(AssistantError) as error:
        next_step(SIGNED_IN, interview(), Model(removal="i2"))

    assert error.value.status == "UNAVAILABLE"


def test_an_invalid_removable_list_is_rejected_before_the_model():
    calls = []

    with pytest.raises(AssistantError) as error:
        next_step(SIGNED_IN, interview(removable="all"), lambda *args: calls.append(args))

    assert error.value.status == "INVALID_ARGUMENT"
    assert calls == []


def test_the_question_prompt_asks_first_whether_to_remove():
    model = Model(remove=False)

    next_step(SIGNED_IN, interview(), model)

    prompt, schema = model.prompts[0]
    assert list(schema["properties"])[0] == "remove"
    assert "set remove to true" in prompt
