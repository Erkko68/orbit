# Security rules and roles

Who may read and write what. [`firebase/firestore.rules`](../../firebase/firestore.rules) enforces it on the server, and [`firebase/tests/rules.test.js`](../../firebase/tests/rules.test.js) checks every rule against the emulator. The app hides what the rules would refuse, but never relies on that.

## Roles

A space is shared, so its items are too: every member creates, edits and completes any item, the way flatmates share one shopping list. Roles only guard what changes the space itself.

| | Owner | Admin | Member |
|---|:---:|:---:|:---:|
| Read the space, its members and items | ✓ | ✓ | ✓ |
| Create, edit and complete any item | ✓ | ✓ | ✓ |
| Delete an item | any | any | only their own |
| Edit the space (name, colour) | ✓ | ✓ | |
| Create, list and revoke invitations | ✓ | ✓ | |
| Change a role between member and admin (never the owner's, never to owner) | ✓ | ✓ | |
| Join with an invitation (#25) | anyone signed in who has a valid invitation, as a plain member | | |
| Remove a member | anyone but the owner | plain members only | |
| Hand ownership to someone else (#27) | ✓ | | |
| Delete the space | ✓ | | |
| Edit their own profile copy, leave the space | ✓ (after handing ownership over) | ✓ | ✓ |

In the app, `Role.managesSpace` (`domain/model/Member.kt`) is the owner-or-admin column: the invitation card shows only when it is true. A child profile (#109) will be a fourth role with less than a member.

## How the rules are organised

```mermaid
flowchart TB
    auth{"signed in?"} -- no --> deny([denied])
    auth -- yes --> path{"path"}
    path -- "users/{uid}" --> own["only uid itself"]
    path -- "spaces/{id}" --> member{"uid in memberIds?"}
    member -- "read" --> ok([allowed])
    member -- "write" --> role["role from members/{uid}"]
    path -- "spaces/{id}/…" --> isMember["isMember(id):<br/>space exists and uid in memberIds"]
    isMember --> checks["role and field checks"]
    path -- "invitations/{code}" --> inv["get by code: anyone signed in<br/>list, create, revoke: owner or admin"]
    path -- "a join batch" --> join["valid invitation in the member document<br/>caller appended to memberIds"]
```

The helper functions at the top of the file:

| Function | True when |
|---|---|
| `isMember(spaceId)` | The space exists and the caller's uid is in its `memberIds` |
| `hasRole(spaceId, uid, roles)` | That member's document has one of `roles` |
| `isOwner`, `isAdminOrOwner` | The caller has that role in the space |
| `isSpaceCreator(spaceId)` | The caller created the space, before or after this write (for the batch that creates it) |
| `keepsCreation()` | The write leaves `createdBy`, `createdAt` and `spaceId` alone |
| `completesAsSelf()` | A write that sets `completedBy` sets it to the caller |
| `isTitle(title)` | A string of 1 to 120 characters after trimming |

## What each collection checks

### `users/{uid}`

Read and written by its owner only.

### `spaces/{spaceId}`

- **Read:** a member.
- **Create:** `memberIds` is exactly `[caller]`, `createdBy` is the caller, `createdAt` is the server time, `name` is a non-blank string, `accent` an integer. Others join through an invitation (#25), never by being written in.
- **Update:** an owner or admin, without changing `createdBy` or `createdAt` and without adding to `memberIds`; or someone joining (below).
- **Delete:** the owner.

### Joining a space

A join is one batch, and the rules accept it only whole (ADR 0002, decision 6). The space document gets the caller appended to `memberIds`, and the caller creates their own member document naming the invitation:

- **The invitation** (`validInvitation`) exists, invites to this space, is not revoked and has not expired. The code comes from the `invitationCode` of the member document.
- **The space update** (`joinsSpace`) is by someone not yet in `memberIds`, changes nothing but `memberIds`, and makes it exactly the old list plus the caller.
- **The member document** (`joinsAsMember`) is the caller's own, a plain `member`, has exactly `role`, `displayName`, an optional `photoUrl`, `joinedAt` (the server time) and `invitationCode`, and the same batch adds the caller to `memberIds`.
- **Joining twice changes nothing.** A member is in `memberIds` already, so the space half is refused and the whole batch fails. Nobody can change `invitationCode` or `joinedAt` afterwards, so an admin who joins again cannot reset their role either.

### `spaces/{spaceId}/members/{uid}`

- **Read:** a member.
- **Create:** the space's creator making their own `owner` membership (in the batch that creates the space), or someone joining (below). An owner or admin cannot add a member directly.
- **Update:** a member changing their own document without changing their role; an owner or admin changing a non-owner between `admin` and `member`. Nobody sets `owner` with an update, and the role of the owner never changes, not even by the owner: ownership moves only through the transfer in #27, which gets its own rule. Never the `invitationCode` or `joinedAt`, which are set once with the join.
- **Delete:** a member leaving (not the owner); the owner removing anyone else; an admin removing a plain member.

### `spaces/{spaceId}/items/{itemId}`

- **Read:** a member.
- **Create:** a member, with `createdBy` the caller, `createdAt` the server time, `spaceId` the path's, a valid title and `status: open`.
- **Update:** a member, keeping the creation fields, a valid title and `status` `open` or `done`; `completedBy` can only become the caller.
- **Delete:** the item's creator, an admin or the owner.

### `invitations/{code}`

- **Get by code:** anyone signed in. The code is the capability.
- **List:** an owner or admin, for their own space only.
- **Create:** an owner or admin of the space, with a code of the right shape, exactly the five fields, `createdBy` the caller, `createdAt` the server time, `expiresAt` in the next 30 days, `revoked: false`.
- **Update:** an owner or admin, changing only `revoked`, and only to `true`.
- **Delete:** nobody, so a revoked link can say it was revoked.

## What the rules do not check

Rules cannot loop, and some checks cost a document read per write. These are left to the app, where the domain validates before writing (`validateItem`):

- The shape of each module (dates, modes, entries). An unknown or malformed module is skipped when read, never trusted.
- That `assignment.assigneeId` is a member.
- The `users/{uid}` fields.
- The size of a space: the plan cap of 8 members is not enforced, so a valid invitation fills a space past it.

## Running the tests

From `firebase/`, with Node and a Java runtime (11 or later) installed:

```sh
npm ci
npx firebase emulators:exec --only firestore "npm test"
```

CI runs the same in the `Firestore rules` job. A change to `firestore.rules` changes the tests, and this page, in the same PR.
