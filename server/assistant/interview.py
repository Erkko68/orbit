"""The AI assistant's interview (#66), without the HTTP server around it so it can be tested with a fake model.

The server keeps no state: each call brings the space's context and every answer so far. The model asks one question
at a time, in the user's language, until it knows enough or the budget is spent; then it proposes a plan, which is
checked before it leaves (#65). Members reach the model by name and a handle for this request (m1, m2), never by uid
or email.
"""

import logging
import os
from collections.abc import Callable
from dataclasses import dataclass
from datetime import date, timedelta
from string import Template

from assistant import PROMPTS
from assistant.proposals import PLAN_SCHEMA, checked_plan

QUESTION_PROMPT = Template((PROMPTS / "question.v2.md").read_text(encoding="utf-8"))
PLAN_PROMPT = Template((PROMPTS / "plan.v2.md").read_text(encoding="utf-8"))

# How many questions an interview may ask, QUESTION_BUDGET in the environment. Once they are answered, the assistant
# proposes with what it has (#74).
QUESTION_BUDGET = int(os.environ.get("QUESTION_BUDGET", "8"))

# How far ahead the plan's prompt lays out the calendar, so the model reads weekdays instead of working them out.
CALENDAR_DAYS = 14

# The languages the app speaks, by the code it sends. Anything else gets English.
LANGUAGES = {"en": "English", "es": "Spanish", "ca": "Catalan", "it": "Italian"}

# What the request may carry, so a caller cannot grow the prompt without bound.
MAX_MEMBERS = 50
MAX_ITEMS = 100
MAX_TEXT = 300

# What the model writes, as JSON Schema. The server adds the id, the position and the budget, which are not the
# model's to decide. [suggestion] is what the model would answer, for "Decide for me". [ready] says it knows enough to
# propose instead.
QUESTION_SCHEMA = {
    "type": "object",
    "properties": {
        "text": {"type": "string"},
        "options": {"type": "array", "items": {"type": "string"}, "maxItems": 5},
        "multiSelect": {"type": "boolean"},
        "freeText": {"type": "boolean"},
        "suggestion": {"type": "string"},
        "ready": {"type": "boolean"},
    },
    "required": ["text", "options", "multiSelect", "freeText", "suggestion", "ready"],
    "additionalProperties": False,
}

# The model's answer to a prompt, as JSON that follows a schema.
Generate = Callable[[str, dict], object]


class AssistantError(Exception):
    """Why the assistant did not answer, with a callable error status: UNAUTHENTICATED, INVALID_ARGUMENT or UNAVAILABLE."""

    def __init__(self, status: str, message: str, details: dict | None = None):
        super().__init__(message)
        self.status = status
        self.message = message
        self.details = details


@dataclass(frozen=True)
class Answer:
    question: str
    selected: list[str]
    text: str
    # The user let the assistant answer with its suggestion (#74).
    decided: bool = False


@dataclass(frozen=True)
class Interview:
    """What a call brings: the answers so far and the space they are about."""

    answers: list[Answer]
    language: str
    today: date
    new_space: bool
    # (handle, name) of each member, in the order the app maps handles back to uids.
    members: list[tuple[str, str]]
    items: list[str]


def parse_interview(data: object) -> Interview:
    """The call's `data`. Raises ValueError when it is not the shape the app sends."""
    if not isinstance(data, dict):
        raise ValueError("data is not an object")
    answers = data.get("answers", [])
    if not isinstance(answers, list) or len(answers) > QUESTION_BUDGET:
        raise ValueError(f"answers must be a list of at most {QUESTION_BUDGET}")
    members = data.get("members", [])
    if not isinstance(members, list) or len(members) > MAX_MEMBERS:
        raise ValueError(f"members must be a list of at most {MAX_MEMBERS}")
    items = data.get("items", [])
    if not isinstance(items, list) or not all(isinstance(title, str) for title in items):
        raise ValueError("items must be a list of titles")
    today = data.get("today")
    language = data.get("language")
    return Interview(
        answers=[_answer(answer) for answer in answers],
        language=LANGUAGES.get(language, "English") if isinstance(language, str) else "English",
        today=date.fromisoformat(today) if isinstance(today, str) else date.today(),
        new_space=data.get("newSpace") is True,
        members=[(f"m{index}", _text(member.get("name") if isinstance(member, dict) else None, "member name"))
                 for index, member in enumerate(members, start=1)],
        items=[title[:MAX_TEXT] for title in items[:MAX_ITEMS]],
    )


def _answer(answer: object) -> Answer:
    if not isinstance(answer, dict):
        raise ValueError("an answer is not an object")
    selected = answer.get("selected", [])
    if not isinstance(selected, list) or not all(isinstance(option, str) for option in selected):
        raise ValueError("selected must be a list of texts")
    text = answer.get("text", "")
    return Answer(
        _text(answer.get("question"), "question"),
        [option[:MAX_TEXT] for option in selected],
        text[:MAX_TEXT] if isinstance(text, str) else "",
        answer.get("decided") is True,
    )


def _text(value: object, what: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"a {what} needs a text")
    return value.strip()[:MAX_TEXT]


def parse_question(answer: object, position: int) -> dict:
    """The question in the model's [answer], in the shape the app parses (#64). Raises ValueError if it is not one."""
    if not isinstance(answer, dict):
        raise ValueError("the answer is not an object")
    text = answer.get("text")
    if not isinstance(text, str) or not text.strip():
        raise ValueError("the question has no text")
    options = answer.get("options")
    if not isinstance(options, list) or not all(isinstance(o, str) and o.strip() for o in options):
        raise ValueError("the options are not a list of texts")
    multi_select = answer.get("multiSelect")
    free_text = answer.get("freeText")
    if not isinstance(multi_select, bool) or not isinstance(free_text, bool):
        raise ValueError("multiSelect and freeText must be true or false")
    if not options and not free_text:
        raise ValueError("the question has no way to answer it")
    options = [o.strip() for o in options]
    suggestion = answer.get("suggestion")
    suggestion = suggestion.strip() if isinstance(suggestion, str) else ""
    # A question with options suggests one of them; one without takes a few words, but only if it can be typed.
    if options and suggestion not in options:
        suggestion = options[0]
    question = {
        "id": f"q{position}",
        "text": text.strip(),
        "options": options,
        "multiSelect": multi_select,
        "freeText": free_text,
        "position": position,
        "budget": QUESTION_BUDGET,
    }
    if suggestion:
        question["suggestion"] = suggestion[:MAX_TEXT]
    return question


def context_text(interview: Interview) -> str:
    """What the model knows about the space, with each member as a handle and a name."""
    if interview.new_space:
        space = "The user is setting up a new space. Until they invite people, they are its only member:"
    else:
        space = "The space already exists. Its members:"
    members = "\n".join(f"- {handle}: {name}" for handle, name in interview.members) or "- (none listed)"
    items = "\n".join(f"- {title}" for title in interview.items) or "- (none yet)"
    return f"{space}\n{members}\n\nItems the space already has:\n{items}"


def answers_text(interview: Interview) -> str:
    """Each question with what the user answered, in order."""
    if not interview.answers:
        return "(nothing asked yet)"
    lines = []
    for answer in interview.answers:
        said = ", ".join(answer.selected + ([answer.text] if answer.text.strip() else [])) or "(skipped)"
        if answer.decided:
            said += " (chosen by the assistant)"
        lines.append(f"Q: {answer.question}\nA: {said}")
    return "\n".join(lines)


def calendar_text(today: date) -> str:
    """The days from [today] on, one per line with its weekday's name and number (1 is Monday), as the schema counts."""
    days = (today + timedelta(days=offset) for offset in range(CALENDAR_DAYS))
    return "\n".join(f"- {day.isoformat()} {day.strftime('%A')} ({day.isoweekday()})" for day in days)


def example_dates(today: date) -> dict[str, str]:
    """The dates of the plan prompt's examples, from [today] on, so the model never sees a date in the past."""

    def next_weekday(number: int) -> date:
        return today + timedelta(days=(number - today.isoweekday()) % 7)

    first = date(today.year + today.month // 12, today.month % 12 + 1, 1)
    days = {"monday": next_weekday(1), "saturday": next_weekday(6), "sunday": next_weekday(6) + timedelta(days=1)}
    return {name: day.isoformat() for name, day in {**days, "first": first}.items()}


def next_step(uid: str | None, data: object, generate: Generate) -> dict:
    """
    The interview's next step for the signed-in caller [uid]: the next question, or `{"plan": …}` once the model has
    enough or the budget is spent. A rejected call never reaches the AI.
    """
    if uid is None:
        raise AssistantError("UNAUTHENTICATED", "Sign in to use the assistant.")
    try:
        interview = parse_interview(data)
    except ValueError as e:
        raise AssistantError("INVALID_ARGUMENT", f"Not an interview: {e}.") from e
    try:
        asked = len(interview.answers)
        if asked < QUESTION_BUDGET:
            answer = generate(_question_prompt(interview), QUESTION_SCHEMA)
            # The first question is always asked: there is nothing to plan from yet.
            if not (asked and isinstance(answer, dict) and answer.get("ready") is True):
                return parse_question(answer, position=asked + 1)
        return {"plan": _plan(interview, generate)}
    except Exception as e:
        logging.exception("The assistant could not take the interview's next step")
        raise AssistantError("UNAVAILABLE", "The assistant could not answer.", details={"reason": "unavailable"}) from e


def _question_prompt(interview: Interview) -> str:
    return QUESTION_PROMPT.substitute(
        context=context_text(interview),
        answers=answers_text(interview),
        remaining=QUESTION_BUDGET - len(interview.answers),
        language=interview.language,
    )


def _plan(interview: Interview, generate: Generate) -> dict:
    """The model's plan, checked. A new space's plan must name it; an existing space's never proposes another."""
    if interview.new_space:
        space_rule = (
            'Also propose the space: a short name for it, as the group would call it ("Flat 4B", "The Garcías"), '
            "and an accent from 0 to 5. Until the user invites people they are its only member, m1: assign items to "
            'm1 or to anyone, and never propose a rotation.'
        )
    else:
        space_rule = "The space exists already: do not propose one."
    prompt = PLAN_PROMPT.substitute(
        context=context_text(interview),
        answers=answers_text(interview),
        today=f"{interview.today.strftime('%A')} {interview.today.isoformat()}",
        calendar=calendar_text(interview.today),
        **example_dates(interview.today),
        space_rule=space_rule,
        language=interview.language,
    )
    plan = generate(prompt, PLAN_SCHEMA)
    if not interview.new_space and isinstance(plan, dict):
        plan = {key: value for key, value in plan.items() if key != "space"}
    plan = checked_plan(plan, {handle for handle, _ in interview.members})
    if interview.new_space and "space" not in plan:
        raise ValueError("the plan for a new space does not name it")
    return plan
