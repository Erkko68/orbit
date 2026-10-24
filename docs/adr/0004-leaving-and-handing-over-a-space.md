# ADR 0004: The space names its owner, and the owner hands over or deletes, never leaves

- Status: Accepted
- Date: 2026-10-24
- Issue: #27

## Context

Members need to leave a space, and an owner needs to hand the space to someone else (#27). ADR 0002 keeps membership in two places, `memberIds` on the space and the `members` subcollection, written in one batch so they never drift, and says there is no `ownerId`: the owner is the member whose role is `owner`.

A transfer is two role changes that must happen together: the new owner becomes `owner` and the old one `admin`. Security rules check each write of a batch on its own, but can read the state after the batch with `getAfter`. To check one half they need to know who the other half is about. Rules cannot loop over the members, so something in the batch has to name the new owner.

#27 also asked what happens when the last member leaves.

## Decision

**The space document names its owner in `ownerId`. A transfer is one batch that sets `ownerId` and both roles. The owner never leaves: they delete the space, or hand it over and then leave as an admin.**

- **`ownerId`** is the creator when the space is made (the create rule requires it) and changes only in the transfer batch. The settings rule refuses any other change to it. Spaces made before this have none until their first transfer, which works the same (the rules read a missing `ownerId` as the caller).
- **The transfer batch** writes `ownerId` to the new owner, `role: owner` on their member document and `role: admin` on the caller's. Each of the three writes checks the other two through `getAfter`, so none is accepted alone. A space never has two owners or none.
- **Leaving** is one batch: the caller out of `memberIds` and their member document deleted. Each half checks the other. The owner's member document cannot be deleted, so the owner cannot leave.
- **The owner deletes the space** from its settings, and everyone else gets "Leave space" there. The last member of a space is always its owner (the owner cannot leave, and the owner can remove anyone else), so "the last member leaves" is this delete.
- **Deleting removes the space document only.** Firestore does not delete subcollections, and the client cannot list every item of a big space in one batch. The members and items left behind are unreadable, because every rule on them asks `isMember`, which needs the space to exist. Invitations to it fail to join for the same reason, and expire.

## Alternatives considered

- **Name the new owner on the caller's own member document** (a `transferredTo` field). It works with the same rules, but leaves a field on a membership that means nothing after the transfer. `ownerId` on the space is a fact worth keeping.
- **Allow an owner to demote themselves without checking for a new owner.** Simpler rules, but one bad write leaves a space with no owner, which nobody could delete or hand over.
- **A callable Cloud Function for the transfer.** The admin SDK skips the rules, so it needs no `ownerId`, but it fails offline, is server code to deploy and test, and every other membership write (create, join) is a batch on the device.
- **Let the last member leave, deleting the space in the same batch.** The owner is always that member, so it is the same delete under another name, with one more rule branch for the owner's own member document.
- **Delete the members and items with the space.** Needs a read of every item and a batch of up to 500 writes, which a big space outgrows, or a Cloud Function on delete. The leftovers cannot be read, so they cost only storage.

## Consequences

- `ownerId` must change only in the transfer batch. The rules enforce it; the repository writes it nowhere else.
- An owner who wants to leave a shared space makes another member the owner first (the "Make owner" choice of a member's role picker), and then leaves as an admin.
- Deleted spaces leave unreadable documents in storage. If that ever matters, a Cloud Function on space delete clears the subcollections; nothing in the app changes.
