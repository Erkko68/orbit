# Architecture decision records

An ADR records one decision that shapes the code: the context, what was decided, what was rejected and what follows from it. The current state of things (the schema, the rules) lives in the reference pages; an ADR keeps the *why*.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-local-persistence.md) | Firestore's offline cache is the only local store: no local database | Accepted |
| [0002](0002-firestore-data-model.md) | The Firestore data model for users, spaces, memberships, items and invitations, and the roles | Accepted, `ownerId` superseded by 0004 |
| [0003](0003-balances-on-device.md) | Balances are derived on the device, in the domain, never stored | Accepted |
| [0004](0004-leaving-and-handing-over-a-space.md) | The space names its owner in `ownerId`; the owner hands over or deletes, never leaves | Accepted, who receives it narrowed by 0009 |
| [0005](0005-bottom-bar-inside-a-space.md) | Only a space has a bottom bar, and its title switches spaces | Accepted |
| [0006](0006-dictation-through-the-platform-recognisers.md) | Voice dictation uses each platform's own speech recogniser, not ML Kit | Accepted |
| [0007](0007-assistant-on-our-own-server.md) | The assistant runs on our own server in Docker, next to its model (Gemma 4) | Accepted, Gemini fallback dropped by #297 |
| [0007](0007-one-item.md) | There is one item, and it can have several people | Accepted |
| [0008](0008-summary-details-and-checklists.md) | An item has a summary and details, and a checklist finishes it | Accepted, two texts superseded by 0010 |
| [0009](0009-only-the-owner-changes-roles.md) | Only the owner changes roles, and the space goes only to an admin | Accepted |
| [0010](0010-standard-navigation-and-one-item-form.md) | Places are screens on the stack, and an item is one fixed form | Accepted |
| [0011](0011-a-recurring-item-rolls-forward.md) | Completing a recurring item rolls the same item forward, and the completion goes to the history | Accepted |
| [0012](0012-three-colours-and-a-flat-page.md) | Three colours on a flat page, and a space is told apart by its people | Accepted |
| [0013](0013-expense-allocation-intent.md) | Expense allocation intent is optional metadata beside authoritative cents | Proposed |
| [0014](0014-item-cost-per-occurrence.md) | An item's cost charges its occurrence once, in the completion batch | Proposed |

## Writing one

Write an ADR when a choice is hard to undo, affects more than one feature, or keeps coming up in review. Number it after the last one and open it with:

```markdown
# ADR 000N: <the decision, as a statement>

- Status: Proposed | Accepted | Superseded by ADR 000M
- Date: YYYY-MM-DD
- Issue: #<issue>

## Context
What forces the decision, and what is already fixed.

## Decision
What we do, and why, in the terms the code uses.

## Alternatives considered
Each option with the reason it lost.

## Consequences
What becomes easier, harder or necessary because of it.
```

An accepted ADR is not rewritten to say something else. When a decision changes, a new ADR supersedes it and the old one's status links to the new one. Small corrections (a revised date, a renamed class) are fine.
