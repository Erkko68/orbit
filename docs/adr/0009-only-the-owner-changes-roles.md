# ADR 0009: Only the owner changes roles, and the space goes only to an admin

- Status: Accepted
- Date: 2026-11-03
- Issue: #261

## Context

ADR 0002 let an owner or an admin change anyone but the owner between `admin` and `member`, and ADR 0004 let the owner hand the space to any member. So an admin could make more admins, and a member nobody had trusted with the space could receive it in one step. Changing a role also sat behind its own step of the settings sheet, a picker, away from the member it was about.

## Decision

**Only the owner changes roles, and only an admin can become the owner.**

- The owner makes a member an admin, and an admin a member again. An admin changes no role: they still invite, edit the space and remove plain members.
- The owner hands the space only to an admin. A plain member is made an admin first, so ownership moves one step at a time.
- The rules enforce both: the role update needs `isOwner`, and both halves of a transfer check that the new owner was an `admin` before it.
- In the app the buttons sit in the member's header in the settings sheet ("Make admin", "Make member", "Make owner"), shown to the owner only. Handing over still asks first, in a dialog.

## Alternatives considered

- **Keep admins changing roles, but not making admins.** An admin could still demote another admin, which turns peers against each other and gives the owner nothing to fall back on.
- **Let the owner hand the space to any member.** One tap would give the space to someone with no say in it before; making them an admin first is one more deliberate step.

## Consequences

- `SpaceMembersViewModel` grants `canChangeRole` and `canTakeOwnership` from `Role.OWNER`, not `Role.managesSpace`.
- An owner who wants to leave makes someone an admin, hands them the space, then leaves as an admin.
