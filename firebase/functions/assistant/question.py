"""A question of the interview, in the shape the app parses (#64)."""

from dataclasses import dataclass

# How many questions an interview may ask. #74 makes it the real budget.
QUESTION_BUDGET = 8

# What the model writes. The function adds the id, the position and the budget, which are not the model's to decide.
MODEL_SCHEMA = {
    "type": "OBJECT",
    "properties": {
        "text": {"type": "STRING"},
        "options": {"type": "ARRAY", "items": {"type": "STRING"}},
        "multiSelect": {"type": "BOOLEAN"},
        "freeText": {"type": "BOOLEAN"},
    },
    "required": ["text", "options", "multiSelect", "freeText"],
}


class InvalidQuestion(ValueError):
    """The provider answered something that is not a question the app can show."""


@dataclass(frozen=True)
class Question:
    id: str
    text: str
    options: list[str]
    multi_select: bool
    free_text: bool
    position: int
    budget: int

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "text": self.text,
            "options": list(self.options),
            "multiSelect": self.multi_select,
            "freeText": self.free_text,
            "position": self.position,
            "budget": self.budget,
        }


def parse_question(answer: object, position: int) -> Question:
    """The question in the model's [answer], as question number [position]. Raises InvalidQuestion if it is not one."""
    if not isinstance(answer, dict):
        raise InvalidQuestion("the answer is not an object")
    text = answer.get("text")
    if not isinstance(text, str) or not text.strip():
        raise InvalidQuestion("the question has no text")
    options = answer.get("options")
    if not isinstance(options, list) or not all(isinstance(o, str) and o.strip() for o in options):
        raise InvalidQuestion("the options are not a list of texts")
    multi_select = answer.get("multiSelect")
    free_text = answer.get("freeText")
    if not isinstance(multi_select, bool) or not isinstance(free_text, bool):
        raise InvalidQuestion("multiSelect and freeText must be true or false")
    if not options and not free_text:
        raise InvalidQuestion("the question has no way to answer it")
    return Question(
        id=f"q{position}",
        text=text.strip(),
        options=[o.strip() for o in options],
        multi_select=multi_select,
        free_text=free_text,
        position=position,
        budget=QUESTION_BUDGET,
    )
