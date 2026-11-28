"""Removal requests (#89): a member asks the assistant to remove things, and it proposes which items to delete.

The app sends only the items the member may delete, each as a handle for this request (i1, i2…) with its title, kind
and the day it was last done, never its id. The model answers with handles and a short reason for each; anything that
is not one of the handles sent is dropped here, so the app only ever sees items it listed.
"""

from dataclasses import dataclass

# What the request may carry, so a caller cannot grow the prompt without bound.
MAX_REMOVABLE = 100
MAX_TITLE = 120
MAX_REASON = 120

# The kinds of item the app sends. Anything else is shown to the model as a plain item.
KINDS = {"task", "list", "event"}

# What the model writes, as JSON Schema: the handle of each item to remove, with why, in the user's language.
REMOVAL_SCHEMA = {
    "type": "object",
    "properties": {
        "items": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {"item": {"type": "string"}, "reason": {"type": "string"}},
                "required": ["item", "reason"],
                "additionalProperties": False,
            },
            "maxItems": MAX_REMOVABLE,
        },
    },
    "required": ["items"],
    "additionalProperties": False,
}


@dataclass(frozen=True)
class Removable:
    """An item the member may delete: its [handle] for this request, what it is called and when it was last done."""

    handle: str
    title: str
    kind: str
    last_done: str = ""


def parse_removable(value: object) -> list[Removable]:
    """The call's `removable`, in order, `i1` first. Raises ValueError when it is not the shape the app sends."""
    if value is None:
        return []
    if not isinstance(value, list) or len(value) > MAX_REMOVABLE:
        raise ValueError(f"removable must be a list of at most {MAX_REMOVABLE}")
    return [_removable(entry, index) for index, entry in enumerate(value, start=1)]


def _removable(entry: object, index: int) -> Removable:
    if not isinstance(entry, dict):
        raise ValueError("a removable item is not an object")
    title = entry.get("title")
    if not isinstance(title, str) or not title.strip():
        raise ValueError("a removable item needs a title")
    kind = entry.get("kind")
    last_done = entry.get("lastDone")
    if last_done is not None and not isinstance(last_done, str):
        raise ValueError("lastDone is not a date")
    return Removable(
        handle=f"i{index}",
        title=title.strip()[:MAX_TITLE],
        kind=kind if kind in KINDS else "item",
        last_done=(last_done or "")[:10],
    )


def removable_text(items: list[Removable]) -> str:
    """The items the model may propose, one per line with its handle, kind and when it was last done."""
    if not items:
        return "- (none: the user may not delete any item of this space)"
    lines = []
    for item in items:
        done = f", last done {item.last_done}" if item.last_done else ", never done"
        lines.append(f'- {item.handle}: "{item.title}" ({item.kind}{done})')
    return "\n".join(lines)


def checked_removal(answer: object, items: list[Removable]) -> dict:
    """
    The model's [answer], as the app reads it: `{"items": [{"item": handle, "reason": …}]}`, each handle one of [items],
    once, in the model's order. A handle that was not sent, or is repeated, is dropped. Raises ValueError when the
    answer is not the schema's shape at all.
    """
    if not isinstance(answer, dict) or not isinstance(answer.get("items"), list):
        raise ValueError("the removal is not a list of items")
    handles = {item.handle for item in items}
    kept: list[dict] = []
    seen: set[str] = set()
    for proposal in answer["items"]:
        if not isinstance(proposal, dict):
            continue
        handle = proposal.get("item")
        if not isinstance(handle, str) or handle.strip() not in handles or handle.strip() in seen:
            continue
        handle = handle.strip()
        seen.add(handle)
        reason = proposal.get("reason")
        kept.append({"item": handle, "reason": reason.strip()[:MAX_REASON] if isinstance(reason, str) else ""})
    return {"items": kept}
