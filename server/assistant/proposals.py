"""What the assistant may propose (#64): the JSON Schemas in schemas/, which the app reads its plans with too."""

import json
import logging
import re
from datetime import date

from assistant import SCHEMAS

# The plan's schema version this server writes. A change an older app cannot read gets a new version.
PLAN_VERSION = 1


def load(name: str) -> dict:
    """The schema in schemas/[name], with each reference to another schema file written out in place: a model gets
    the schema whole, and cannot open a file."""
    return _inline(json.loads((SCHEMAS / name).read_text(encoding="utf-8")))


def _inline(node: object) -> object:
    if isinstance(node, list):
        return [_inline(child) for child in node]
    if not isinstance(node, dict):
        return node
    if "$ref" in node:
        referenced = {key: value for key, value in load(node["$ref"]).items() if key != "$schema"}
        return {**referenced, **{key: _inline(value) for key, value in node.items() if key != "$ref"}}
    return {key: _inline(value) for key, value in node.items()}


# What #66 asks the model to write at the end of an interview, and #65 checks its answer against.
PLAN_SCHEMA = load(f"plan.v{PLAN_VERSION}.json")
SPACE_SCHEMA = PLAN_SCHEMA["properties"]["space"]
ITEM_SCHEMA = PLAN_SCHEMA["properties"]["items"]["items"]

# The item rules the app's validateItem has too.
TITLE_MAX_LENGTH = 120


def checked_plan(plan: object, handles: set[str]) -> dict:
    """The model's [plan] without the items that break the schemas or the rules, each logged with why. [handles] are
    the members the request names (m1, m2). Raises ValueError when the plan itself is unusable: not a plan of this
    version, or a space that is not one."""
    if not isinstance(plan, dict) or plan.get("version") != PLAN_VERSION or not isinstance(plan.get("items"), list):
        raise ValueError(f"not a plan of version {PLAN_VERSION}")
    shape = {key: value for key, value in plan.items() if key != "items"}
    plan_errors = schema_errors({**shape, "items": []}, PLAN_SCHEMA)
    if "space" in plan:
        plan_errors += _space_errors(plan["space"])
    if plan_errors:
        raise ValueError("; ".join(plan_errors))
    items = []
    for index, item in enumerate(plan["items"]):
        errors = item_errors(item, handles)
        if errors:
            logging.warning("Dropped proposal %d of the plan: %s", index, "; ".join(errors))
        else:
            items.append(item)
    return {**plan, "items": items}


def _space_errors(space: object) -> list[str]:
    if isinstance(space, dict) and isinstance(space.get("name"), str) and not space["name"].strip():
        return ["space.name is blank"]
    return []


def item_errors(item: object, handles: set[str]) -> list[str]:
    """Why [item] is not a proposal the app can show: its schema first, then the rules across its fields."""
    errors = schema_errors(item, ITEM_SCHEMA)
    return errors or _rule_errors(item, handles)


def _rule_errors(item: dict, handles: set[str]) -> list[str]:
    modules = item["modules"]
    return _title_errors(item["title"]) + _date_errors(modules) + _people_and_entry_errors(modules, handles)


def _title_errors(title: str) -> list[str]:
    title = title.strip()
    if not title:
        return ["the title is blank"]
    if len(title) > TITLE_MAX_LENGTH:
        return ["the title is too long"]
    return []


def _date_errors(modules: dict) -> list[str]:
    """The rules of the schedule, and of what needs one: a recurrence and a reminder."""
    schedule = modules.get("schedule")
    if not schedule:
        return [f"a {module} needs a schedule" for module in ("recurrence", "reminder") if module in modules]
    until_text = modules.get("recurrence", {}).get("until")
    try:
        start = date.fromisoformat(schedule["date"])
        end = date.fromisoformat(schedule["endDate"]) if "endDate" in schedule else None
        until = date.fromisoformat(until_text) if until_text else None
    except ValueError:
        return ["a date is not on the calendar"]
    errors = []
    if end and "time" in schedule:
        errors.append("a schedule with an endDate has no time")
    if end and end <= start:
        errors.append("the endDate is not after the date")
    if until and until < start:
        errors.append("the recurrence ends before the date")
    return errors


def _people_and_entry_errors(modules: dict, handles: set[str]) -> list[str]:
    errors = []
    outsiders = set(modules.get("assignment", {}).get("assignees", [])) - handles
    if outsiders:
        errors.append(f"{', '.join(sorted(outsiders))} not in the space")
    if any(not entry["text"].strip() for entry in modules.get("checklist", {}).get("entries", [])):
        errors.append("a checklist entry is blank")
    return errors


def schema_errors(value: object, schema: dict, path: str = "") -> list[str]:
    """Where [value] breaks [schema], in the part of JSON Schema that schemas/ uses. The server's runtime has only the
    standard library (ADR 0007); the tests check this agrees with the jsonschema package."""
    where = path or "the value"
    if "anyOf" in schema:
        if all(schema_errors(value, option, path) for option in schema["anyOf"]):
            return [f"{where} matches none of its shapes"]
        return []
    expected = schema.get("type")
    if expected and not _is_type(value, expected):
        return [f"{where} is not of type {expected}"]
    if "enum" in schema and value not in schema["enum"]:
        return [f"{where} is not one of {schema['enum']}"]
    if isinstance(value, str):
        return _string_errors(value, schema, where)
    if _is_type(value, "number"):
        return _number_errors(value, schema, where)
    if isinstance(value, list):
        return _array_errors(value, schema, path)
    if isinstance(value, dict):
        return _object_errors(value, schema, path)
    return []


def _string_errors(value: str, schema: dict, where: str) -> list[str]:
    errors = []
    if len(value) < schema.get("minLength", 0):
        errors.append(f"{where} is too short")
    if "maxLength" in schema and len(value) > schema["maxLength"]:
        errors.append(f"{where} is too long")
    if "pattern" in schema and not re.search(schema["pattern"], value):
        errors.append(f"{where} does not match {schema['pattern']}")
    return errors


def _number_errors(value: int | float, schema: dict, where: str) -> list[str]:
    errors = []
    if "minimum" in schema and value < schema["minimum"]:
        errors.append(f"{where} is below {schema['minimum']}")
    if "maximum" in schema and value > schema["maximum"]:
        errors.append(f"{where} is above {schema['maximum']}")
    return errors


def _array_errors(value: list, schema: dict, path: str) -> list[str]:
    where = path or "the value"
    errors = []
    if len(value) < schema.get("minItems", 0):
        errors.append(f"{where} has too few entries")
    if schema.get("uniqueItems") and len({json.dumps(entry, sort_keys=True) for entry in value}) < len(value):
        errors.append(f"{where} repeats an entry")
    for index, entry in enumerate(value if "items" in schema else []):
        errors += schema_errors(entry, schema["items"], f"{path}[{index}]")
    return errors


def _object_errors(value: dict, schema: dict, path: str) -> list[str]:
    properties = schema.get("properties", {})
    errors = [f"{_join(path, key)} is missing" for key in schema.get("required", []) if key not in value]
    for key, entry in value.items():
        if key in properties:
            errors += schema_errors(entry, properties[key], _join(path, key))
        elif schema.get("additionalProperties") is False:
            errors.append(f"{_join(path, key)} is not allowed")
    return errors


_TYPES = {"object": dict, "array": list, "string": str, "integer": int, "number": (int, float), "boolean": bool}


def _is_type(value: object, expected: str) -> bool:
    # A bool is an int in Python, but not in JSON.
    if isinstance(value, bool):
        return expected == "boolean"
    return isinstance(value, _TYPES[expected])


def _join(path: str, key: str) -> str:
    return f"{path}.{key}" if path else key
