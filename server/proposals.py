"""What the assistant may propose (#64): the JSON Schemas in schemas/, which the app reads its plans with too."""

import json
from pathlib import Path

SCHEMAS = Path(__file__).parent / "schemas"

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
