# Architecture decision records

An ADR records one decision that shapes the code: the context, what was decided, what was rejected and what follows from it. The current state of things (the schema, the rules) lives in the reference pages; an ADR keeps the *why*.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-local-persistence.md) | Firestore's offline cache is the only local store: no local database | Accepted |
| [0002](0002-firestore-data-model.md) | The Firestore data model for users, spaces, memberships, items and invitations, and the roles | Accepted, `ownerId` superseded by 0004 |
| [0003](0003-balances-on-device.md) | Balances are derived on the device, in the domain, never stored | Accepted |
| [0004](0004-leaving-and-handing-over-a-space.md) | The space names its owner in `ownerId`; the owner hands over or deletes, never leaves | Accepted |

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
