# ADR 0007: There is one item, and it can have several people

- Status: Accepted
- Date: 2026-11-02
- Issue: #242

## Context

ADR 0002 already said there is no type per item kind, and that a preset is only a label: behaviour comes from the modules. The app did not keep to it. The item screen branched on `Preset.SHOPPING_LIST`, so a task with a checklist looked and behaved differently from a list with the same entries, and a list could not be completed at all. Choosing a kind before typing a name forced a decision that means nothing: a chore can also be a list, and an event is any item with a date and a reminder.

Assigning had grown three modes (`member`, `anyone`, `claim`) that held one person at most. A household often shares a job between two people, and first to claim only added a step: tapping your own face says the same thing.

## Decision

**There is one item: a title, a description and the modules it needs. Presets are gone. An assignment is either some members or anyone.**

- **No kinds.** `Preset`, `Item.preset` and the add sheet go. What an item can do comes from what it has: anything with a checklist shows its entries and progress, anything can have a description, and anything can be completed.
- **Add opens the item.** The add button opens the item screen on a new item. It is written to Firestore once it has a name, and leaving it without one leaves nothing behind, so no other member ever sees a blank draft.
- **Several people.** `Assignment.ToMembers` holds a set of ids, stored as `assigneeIds`. Anyone stays as its own answer: it shows the whole space and belongs to nobody in particular. No assignment module means it has not been decided yet.
- **First to claim goes.** Picking yourself does the same with one step fewer. Existing documents keep working: a claimed item reads as that member's, an unclaimed one as anyone's.
- **One item has one status.** Whoever completes it completes it for everyone. There is no per-person completion.

## Alternatives considered

- **Keep the presets as starting points.** A "List" tile would save one tap, but it keeps the question the app no longer asks, and every preset would still need a label, an icon and a filter.
- **Create the item on tap with a placeholder title, and delete it on leaving if untouched.** Simpler to write, but other members would see "New item" appear and vanish, and a killed app would leave it behind for good.
- **One assignee only, as Asana does**, so that responsibility is never shared and nobody assumes someone else has it. Right for a team with a task owner, but a household does split jobs. A person who is not the only assignee still sees the item as theirs, which limits the same risk.
- **Complete per person.** Each assignee ticks their own part. It needs a status per member and a rule for when the item is done. The checklist already splits a job into parts.

## Consequences

- The Tasks tab sorts by what items have: open items with a checklist are tiles, everything else is a row. Its filters are All, Mine and Lists.
- An item must be named before anything else is added to it.
- A stored `preset` is ignored when read, and old `assigneeId` and `claim` values are still read. Nothing is migrated.
- Chores (#52) and bills (#59) become modules on the one item, not presets. Templates in Sprint 3 start from a saved set of modules.
- Anyone and no assignment behave the same for reminders and the agenda. They differ only in what the screen shows, and a later change may merge them.
