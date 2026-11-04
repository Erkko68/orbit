# ADR 0011: Completing a recurring item rolls the same item forward

- Status: Accepted
- Date: 2026-11-04
- Issue: #45

## Context

A recurrence repeats an item's schedule: every N days, every N weeks on chosen weekdays, or every N months on a day of the month, until an optional end date (`modules.recurrence`, which needs `modules.schedule`). Something has to happen when a member completes one. Assignment and rotation (#49 to #51), reminders (#40) and the item's history (#47) all work on one item document, and the history already records each completion in `spaces/{id}/completions`.

## Decision

**The same item rolls forward: its schedule moves to the next occurrence and it stays open, and the completion goes to the history.**

- One batch writes the completion and the new `modules.schedule`; `status` stays `open`. The next date is `nextOccurrence(schedule, recurrence, after)`, a pure domain use case.
- `after` is the later of the current occurrence's date and the day it is completed. The rhythm is kept (every Monday stays on Mondays), and a late completion never lands on a date that is already past.
- The time, the duration and the length of a window move with the date.
- A monthly rule on a day a month does not have falls on that month's last day, and comes back to its day the month after (the 31st: 28 February, then 31 March).
- When there is no next occurrence before `until`, completing it closes the item as usual (`status: done`).
- Occurrences count from the schedule's date: weeks from the week it falls in, starting on Monday, and months from its month.

## Alternatives considered

- **A new item per occurrence.** The assistant, rotation, reminders and the history would each have to follow a chain of documents, and an edit (a new assignee, a checklist entry) would not carry to the next one.
- **Generate future occurrences up front.** Many documents for one chore, and an edit to the rule has to rewrite or delete them all.
- **Next date counted from the completion day** ("3 days after I last did it"). Simpler, but a weekly chore drifts off its day. It can come later as a separate option if a space asks for it.

## Consequences

- The item's history is the record of past occurrences: each completion stays there while the item moves on. Which occurrence a completion was for is recorded with the card (#46).
- A recurring item is never `done` until its end, so reopening applies only to that last completion; undoing a roll-forward belongs to the activity history (#84).
- Rotation (#49) hands the item to the next member in the same batch, on the same document.
- Weeks start on Monday for every space, whatever the phone's first day of the week: it only matters to a rule every 2 or more weeks.
