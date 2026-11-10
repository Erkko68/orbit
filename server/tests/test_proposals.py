import json

import pytest
from jsonschema import Draft202012Validator

from assistant import SCHEMAS
from assistant.proposals import PLAN_SCHEMA, PLAN_VERSION

# The app parses the same samples (ProposalSamplesTest), so both sides read one contract.
SAMPLES = sorted((SCHEMAS / "samples").glob("*.json"))


def read(path) -> object:
    return json.loads(path.read_text(encoding="utf-8"))


def errors(plan: object) -> list:
    return list(Draft202012Validator(PLAN_SCHEMA).iter_errors(plan))


def item_with(**modules) -> dict:
    return {"title": "Take out the bins", "modules": modules}


def plan_with(item: dict | None = None, **changes) -> dict:
    return {"version": PLAN_VERSION, "items": [item or item_with()], **changes}


@pytest.mark.parametrize("path", sorted(SCHEMAS.glob("*.json")), ids=lambda path: path.name)
def test_every_schema_is_a_json_schema(path):
    Draft202012Validator.check_schema(read(path))


def test_the_plan_schema_is_whole_for_the_model():
    assert "$ref" not in json.dumps(PLAN_SCHEMA)
    Draft202012Validator.check_schema(PLAN_SCHEMA)
    space = PLAN_SCHEMA["properties"]["space"]
    assert space["title"] == "Space proposal, version 1"
    assert space["description"] == "The new space, only when the interview is for one."
    assert "$schema" not in space
    assert PLAN_SCHEMA["properties"]["items"]["items"]["title"] == "Item proposal, version 1"


def test_the_plan_schema_is_the_version_the_server_writes():
    assert PLAN_SCHEMA["properties"]["version"]["enum"] == [PLAN_VERSION]


def test_there_are_samples_for_each_language():
    assert {path.name.split(".")[1] for path in SAMPLES} == {"es", "ca", "en"}


@pytest.mark.parametrize("path", SAMPLES, ids=lambda path: path.name)
def test_every_sample_is_a_plan(path):
    assert errors(read(path)) == []


def test_a_plan_needs_no_space_and_an_item_no_modules():
    assert errors(plan_with()) == []


@pytest.mark.parametrize(
    "recurrence",
    [
        {"unit": "day", "every": 2},
        {"unit": "week", "every": 1, "weekdays": [1, 4]},
        {"unit": "month", "every": 3, "dayOfMonth": 31, "until": "2027-12-31"},
    ],
)
def test_an_item_repeats_by_days_weeks_or_months(recurrence):
    assert errors(plan_with(item=item_with(schedule={"date": "2026-11-09"}, recurrence=recurrence))) == []


@pytest.mark.parametrize(
    "plan",
    [
        {"items": []},
        plan_with(version=2),
        plan_with(items="Take out the bins"),
        plan_with(space={"name": "Flat"}),
        plan_with(space={"name": "", "accent": 0}),
        plan_with(space={"name": "Flat", "accent": 6}),
        plan_with(item={"modules": {}}),
        plan_with(item={"title": "x" * 121, "modules": {}}),
        plan_with(item={"title": "Take out the bins"}),
        plan_with(item={"title": "Take out the bins", "modules": {}, "priority": "high"}),
        plan_with(item=item_with(recurrence={"every": "week"})),
        plan_with(item=item_with(recurrence={"unit": "year", "every": 1})),
        plan_with(item=item_with(recurrence={"unit": "day", "every": 0})),
        plan_with(item=item_with(recurrence={"unit": "day", "every": 1, "weekdays": [1]})),
        plan_with(item=item_with(recurrence={"unit": "week", "every": 1})),
        plan_with(item=item_with(recurrence={"unit": "week", "every": 1, "weekdays": []})),
        plan_with(item=item_with(recurrence={"unit": "week", "every": 1, "weekdays": [8]})),
        plan_with(item=item_with(recurrence={"unit": "week", "every": 1, "weekdays": [1, 1]})),
        plan_with(item=item_with(recurrence={"unit": "month", "every": 1})),
        plan_with(item=item_with(recurrence={"unit": "month", "every": 1, "dayOfMonth": 32})),
        plan_with(item=item_with(recurrence={"unit": "day", "every": 1, "until": "31/12/2027"})),
        plan_with(item=item_with(schedule={"time": "21:00"})),
        plan_with(item=item_with(schedule={"date": "9/11/2026"})),
        plan_with(item=item_with(schedule={"date": "2026-11-09", "time": "25:00"})),
        plan_with(item=item_with(schedule={"date": "2026-11-09", "durationMinutes": 0})),
        plan_with(item=item_with(assignment={"mode": "member"})),
        plan_with(item=item_with(assignment={"mode": "member", "assignees": []})),
        plan_with(item=item_with(assignment={"mode": "member", "assignees": ["Ana"]})),
        plan_with(item=item_with(assignment={"mode": "member", "assignees": ["m1", "m1"]})),
        plan_with(item=item_with(assignment={"mode": "anyone", "assignees": ["m1"]})),
        plan_with(item=item_with(assignment={"mode": "claim"})),
        plan_with(item=item_with(checklist={"entries": []})),
        plan_with(item=item_with(checklist={"entries": [{"text": "Milk", "quantity": 0}]})),
        plan_with(item=item_with(reminder={"offsetsMinutes": [-10]})),
    ],
)
def test_rejects_what_is_not_a_plan(plan):
    assert errors(plan) != []
