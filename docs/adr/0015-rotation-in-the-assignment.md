# ADR 0015: A rotation is an assignment whose current turn is the one assignee

- Status: Proposed
- Date: 2026-11-16
- Issues: #49

## Context

A chore that repeats should not always fall on the same person. A recurring item rolls forward when it is completed (ADR 0011), an assignment holds a set of members (ADR 0007) and a member can mark themselves away (#48). Rotation joins the three: when a recurring item is completed, the next occurrence goes to the next member who is around.

Everything that reads an item (the agenda, the "mine" filter, the turn card, reminders, a member's page) already works from `assigneeIds`. Adding a second place that says who has an item would mean teaching each of them about it.

## Decision

**A rotation is a kind of assignment. Its order is stored next to it, and its current turn is the one member in `assigneeIds`.**

- `modules.assignment` has a third mode, `rotation`, with `assigneeIds` holding the one member whose turn it is and `rotation`, the list of uids in the order they follow. `Assignment.Rotation(order, current)` in the domain.
- `nextTurn(rotation, members, on)` is a pure use case. It returns the next member in the order, after the current one and wrapping round, who is still in the space and not away on the date of the next occurrence. Members who left are skipped. When everyone else is away the current member keeps the item, and when the current member left too, the next one in the order takes it whether away or not.
- Completing a recurring item in rotation changes `assigneeIds` in the same batch that moves the schedule forward and records the completion (ADR 0011). The repository reads the members from Firestore's cache, as it does for a cost (ADR 0014), so completing works offline. The device that completes decides, with the away periods it has.
- Completing an item that does not recur ends it and leaves the turn as it is. Reopening never moves the turn.
- The Who sheet offers it: "Take turns" starts a rotation on the members who have the item, or on everyone, tapping a member gives them the turn, and arrows reorder them. Turning it off leaves the item with the member whose turn it is.
- The rules do not look inside modules, so they need no change. The app checks that a rotation has two or more different members and that the current one is among them (`InvalidRotation`), and that the current one is in the space.

## Alternatives considered

- **A separate `rotation` module next to the assignment**: two modules could disagree about who has the item, and every reader would have to merge them.
- **Store the turn as an index into the order**: a member leaving or being reordered would silently pass the turn to someone else. Naming the member is stable.
- **Pass the turn to the next member whether away or not**: the point of #48 is that the chore does not wait for someone on holiday.

## Consequences

- Items need no migration. An older build reads a rotation as an unknown assignment mode and skips the module, so on it the item looks unassigned until it is updated.
- A member who left stays in the stored order and is skipped. They are dropped from it the next time the order is edited.
- Two phones completing the same occurrence offline at once can pass the turn twice. The owner of the item gives it back from the Who sheet.
- A rotation that does not hold together (fewer than two members, a turn outside the order) reads as an assignment to the member whose turn it is.
