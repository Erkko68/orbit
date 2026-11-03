# ADR 0008: An item has a summary and details, and a checklist finishes it

- Status: Accepted
- Date: 2026-11-02
- Issue: #242

## Context

ADR 0007 made every item the same, with one Complete button. On anything with a checklist that button sat next to the field that adds entries, and it asked a question lists do not need. Apps that keep a list (Google Keep, Apple Reminders, Bring!, AnyList) show no complete action for a whole list in their store screens. Task apps put it next to the title or in the top bar. The description had also become a form field in its own row, away from the title it belongs to.

## Decision

**A short summary sits under the title, long details fill the screen, and a checklist completes its item by itself.**

- **Two texts.** `description` is the summary. A new optional `details` field holds the long text. Without a checklist it fills the screen, and with one it is a row under the entries.
- **Mark as done only without a checklist.** It is a pill in the header, which turns into Reopen.
- **A checklist finishes itself.** Ticking the last entry completes the item. Unticking an entry or adding one reopens it. Removing the last open entry completes it too.
- **One list.** Entries are not split in two: what is left comes first, what is done follows, and the row that adds is the first row. The header counts them.
- **A checklist has no turn.** The turn card's one Done button would complete a list with entries left, so lists stay out of it.

## Alternatives considered

- **A floating toolbar for add, date and reminder.** Drawn and dropped: it kept a second thing floating over the list, and the date and reminder read better as rows.
- **A list never completes.** Closest to the list apps, but it splits lists from tasks again, which ADR 0007 removed.
- **Keep one text.** A long text in the header pushes the entries off the screen, and a short one leaves no room for instructions.

## Consequences

- The status follows the entries through two writes from the client, the entry and then the item. Two members ticking at the same moment could leave a list open with everything ticked, until the next tick.
- A checklist cannot be finished with entries left: tick them or remove them. Reopening a finished list from the Tasks row leaves its entries ticked.
- No migration and no rules change: `details` is optional, and the rules do not limit an item's fields.
