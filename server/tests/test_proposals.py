import json

import pytest
from jsonschema import Draft202012Validator

from assistant import SCHEMAS
from assistant.proposals import PLAN_SCHEMA, PLAN_VERSION, checked_plan, item_errors, schema_errors

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


NOT_PLANS = [
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
]


@pytest.mark.parametrize("plan", NOT_PLANS)
def test_rejects_what_is_not_a_plan(plan):
    assert errors(plan) != []


# The server's own check (#65) runs on the standard library, so it must agree with jsonschema on what is a plan.
@pytest.mark.parametrize("plan", NOT_PLANS)
def test_the_servers_check_rejects_what_jsonschema_rejects(plan):
    assert schema_errors(plan, PLAN_SCHEMA) != []


@pytest.mark.parametrize("path", SAMPLES, ids=lambda path: path.name)
def test_the_servers_check_takes_every_sample_whole(path):
    plan = read(path)

    assert schema_errors(plan, PLAN_SCHEMA) == []
    assert checked_plan(plan, handles={"m1", "m2", "m3"}) == plan


SCHEDULED = {"date": "2026-11-09"}


@pytest.mark.parametrize(
    ("item", "why"),
    [
        (item_with(cost={"amount": 1200}), "modules.cost is not allowed"),
        (item_with(assignment={"mode": "member", "assignees": ["m1", "m4"]}), "m4 not in the space"),
        (item_with(schedule={"date": "2026-11-09", "endDate": "2026-11-08"}), "the endDate is not after the date"),
        (item_with(schedule={"date": "2026-11-09", "endDate": "2026-11-09"}), "the endDate is not after the date"),
        ({"title": "   ", "modules": {}}, "the title is blank"),
        ({"title": "", "modules": {}}, "title is too short"),
        (item_with(schedule={"date": "2026-11-09", "time": "21:00", "endDate": "2026-11-12"}), "has no time"),
        (item_with(schedule={"date": "2026-02-30"}), "a date is not on the calendar"),
        (item_with(recurrence={"unit": "day", "every": 1}), "a recurrence needs a schedule"),
        (
            item_with(schedule=SCHEDULED, recurrence={"unit": "day", "every": 1, "until": "2026-11-08"}),
            "the recurrence ends before the date",
        ),
        (item_with(reminder={"offsetsMinutes": [60]}), "a reminder needs a schedule"),
        (item_with(checklist={"entries": [{"text": " "}]}), "a checklist entry is blank"),
    ],
)
def test_finds_why_an_item_cannot_be_proposed(item, why):
    assert any(why in error for error in item_errors(item, handles={"m1", "m2"}))


def test_keeps_an_item_that_follows_every_rule():
    item = item_with(
        schedule={"date": "2026-11-09", "endDate": "2026-11-12"},
        recurrence={"unit": "week", "every": 1, "weekdays": [1], "until": "2026-11-09"},
        assignment={"mode": "member", "assignees": ["m2"]},
        checklist={"entries": [{"text": "Milk", "quantity": 2}]},
        reminder={"offsetsMinutes": [0]},
    )

    assert item_errors(item, handles={"m1", "m2"}) == []


def test_drops_the_invalid_proposals_and_logs_why(caplog):
    good = item_with(assignment={"mode": "anyone"})
    plan = plan_with(items=[good, {"title": " ", "modules": {}}, item_with(cost={})], space={"name": "Flat", "accent": 2})

    checked = checked_plan(plan, handles=set())

    assert checked == {**plan, "items": [good]}
    assert "Dropped proposal 1 of the plan: the title is blank" in caplog.text
    assert "Dropped proposal 2 of the plan: modules.cost is not allowed" in caplog.text


@pytest.mark.parametrize(
    "plan",
    [
        "a plan",
        {"items": []},
        plan_with(version=2),
        plan_with(items={}),
        plan_with(space={"name": "Flat", "accent": 6}),
        plan_with(space={"name": "  ", "accent": 0}),
        plan_with(owner="m1"),
    ],
)
def test_a_plan_that_is_not_one_is_not_used(plan):
    with pytest.raises(ValueError):
        checked_plan(plan, handles={"m1"})
