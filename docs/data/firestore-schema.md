# Firestore schema

The database as it is today: every collection, field, query and write. Why it has this shape is in [ADR 0002](../adr/0002-firestore-data-model.md); who may read and write it is in [Security rules and roles](security-rules.md). A change to the schema updates this page in the same pull request.

## Entities

```mermaid
erDiagram
    USER ||--o{ MEMBER : "is, in each space"
    SPACE ||--|{ MEMBER : "has (memberIds mirrors them)"
    SPACE ||--o{ ITEM : contains
    SPACE ||--o{ EXPENSE : "keeps its ledger in"
    ITEM ||--o{ COMPLETION : "has been completed in"
    SPACE ||--o{ INVITATION : "invites through"
    USER ||--o{ INVITATION : creates
    MEMBER ||--o{ ITEM : "creates, completes, is assigned"

    USER {
        string uid PK "Firebase Auth uid"
        string displayName
        string photoUrl "optional"
        string language "es, ca, it or en"
        string timeZone "IANA id"
        timestamp createdAt
    }
    SPACE {
        string spaceId PK "auto id, made on the device"
        string name
        int accent "index into spaceAccents"
        array memberIds "uids"
        map formerMembers "uid to name, optional"
        string createdBy FK
        string ownerId FK "the member whose role is owner"
        timestamp createdAt
    }
    MEMBER {
        string uid PK "same as the user"
        string role "owner, admin or member"
        string displayName "copy of the profile"
        string photoUrl "copy, optional"
        timestamp joinedAt
    }
    ITEM {
        string itemId PK "auto id, made on the device"
        string spaceId FK "copy of the parent"
        string title
        string description "optional"
        string details "optional"
        string status "open or done"
        string createdBy FK
        timestamp createdAt
        string completedBy FK "optional"
        timestamp completedAt "optional"
        map modules
    }
    COMPLETION {
        string completionId PK "auto id, made on the device"
        string itemId FK
        string completedBy FK
        timestamp completedAt
    }
    EXPENSE {
        string expenseId PK "auto id, made on the device"
        string spaceId FK "copy of the parent"
        string title
        string paidBy FK
        map split "uid to cents"
        string date "YYYY-MM-DD"
        string kind "expense or settlement"
        string itemId FK "optional"
        string createdBy FK
        timestamp createdAt
    }
    INVITATION {
        string code PK "8 characters"
        string spaceId FK
        string createdBy FK
        timestamp createdAt
        timestamp expiresAt
        bool revoked
    }
```

## Paths

```
users/{uid}                          private profile
spaces/{spaceId}                     the space, with the list of member ids
spaces/{spaceId}/members/{uid}       one membership: role and public profile copy
spaces/{spaceId}/items/{itemId}      one item: core fields and its modules
spaces/{spaceId}/completions/{completionId}  one time an item was completed
spaces/{spaceId}/expenses/{expenseId}  one entry of the ledger
invitations/{code}                   an invitation to one space; the id is the code
```

The collection names are constants in `data/repository/Firestore.kt` (`USERS`, `SPACES`, `MEMBERS`, `ITEMS`, `COMPLETIONS`, `EXPENSES`, `INVITATIONS`).

## Conventions

- Field names are camelCase. Enum values are lowercase strings (`shopping_list`, `owner`).
- An optional field is left out, never stored as `null`. Documents are written with `encodeDefaults = false`.
- Ids: `users` and `members` use the Firebase Auth uid. Spaces and items use auto ids made on the device, so creating them works offline. An invitation's id is its code.
- Instants are Firestore timestamps set to the server time. While a write is pending offline the server time is missing, so the app reads timestamps with the *estimate* behaviour (`DocumentSnapshot.instant()`).
- Calendar dates and times of day are wall-clock strings (`2026-10-09`, `20:00`), read in the time zone of the phone showing them.
- A reference to another document is its id in a field (`spaceId`, `createdBy`). No `DocumentReference` fields.

## Collections

### `users/{uid}`

The signed-in user's private profile. Only its owner reads or writes it.

| Field | Type | Required | Notes |
|---|---|---|---|
| `displayName` | string | yes | |
| `photoUrl` | string | no | From Google sign-in |
| `language` | string | yes | `es`, `ca`, `it` or `en`, from the phone. Refreshed on every sign-in. For the server only: the app reads the phone's. |
| `timeZone` | string | yes | IANA id, from the phone. Same as `language`. |
| `createdAt` | timestamp | yes | Server time |
| `aiConsentAt` | timestamp | no | Server time when the user accepted what the assistant sends to the AI provider (#71). Missing until then, so the notice is asked once per account, on any device. Withdrawing it is #112. |

The email is not stored: it comes from the Firebase Auth account.

### `spaces/{spaceId}`

| Field | Type | Required | Notes |
|---|---|---|---|
| `name` | string | yes | Not blank |
| `accent` | integer | yes | Index into `OrbitTheme.colors.spaceAccents`, not a color |
| `memberIds` | array of uid | yes | Kept in step with the `members` subcollection, in the same batch. At most 8 (plan cap). |
| `formerMembers` | map of uid to string | no | The name of each member who left, written with the leave. Their member document is gone, but their balance still shows. A member who rejoins keeps an entry, and their member document wins. |
| `createdBy` | uid | yes | Never changes |
| `ownerId` | uid | yes | The member whose role is `owner`. The creator at first; changes only in the transfer batch, with the two roles (ADR 0004). Spaces made before #27 have none until their first transfer. |
| `createdAt` | timestamp | yes | Server time, never changes |

### `spaces/{spaceId}/members/{uid}`

| Field | Type | Required | Notes |
|---|---|---|---|
| `role` | string | yes | `owner`, `admin` or `member`. Exactly one `owner` per space. An unknown value reads as `member`. |
| `displayName` | string | yes | Copy of the user's profile, updated in the same batch as the profile |
| `photoUrl` | string | no | Copy of the user's profile |
| `joinedAt` | timestamp | yes | Server time |
| `invitationCode` | string | only for a member who joined | The invitation that let them in. Written with the join, which the rules check against it; never changed. |

### `spaces/{spaceId}/items/{itemId}`

| Field | Type | Required | Notes |
|---|---|---|---|
| `spaceId` | string | yes | Copy of the parent id, so a collection-group query stays possible. Never changes. |
| `title` | string | yes | 1 to 120 characters after trimming |
| `description` | string | no | A short summary under the title |
| `details` | string | no | The long text of the item |
| `status` | string | yes | `open` or `done`. New items are `open`. |
| `createdBy` | uid | yes | Never changes |
| `createdAt` | timestamp | yes | Server time, never changes |
| `completedBy` | uid | with `done` | Set with `status: done` to the member who completed it, removed on reopen |
| `completedAt` | timestamp | with `done` | Server time, same lifecycle as `completedBy` |
| `modules` | map | yes | One key per module present. Unknown keys are kept and skipped. |

#### `modules.schedule`

| Field | Type | Required | Notes |
|---|---|---|---|
| `date` | string | yes | `YYYY-MM-DD`: the due date, or the first day of a window |
| `time` | string | no | `HH:mm`. Absent means all day. Not with `endDate`. |
| `endDate` | string | no | `YYYY-MM-DD`, after `date`: due any time in the window |
| `durationMinutes` | integer | no | Greater than 0 |

The *deadline* is `endDate`, or `date` without one: an open item is overdue once it is before today. The *due moment*, for reminders, is the deadline at `time`, or at 09:00 without one.

#### `modules.assignment`

| Field | Type | Required | Notes |
|---|---|---|---|
| `mode` | string | yes | `member`, `anyone` or `claim`. Unknown skips the module. |
| `assigneeId` | uid | for `member` | For `claim`, absent until someone claims it. Never for `anyone`. Must be in `memberIds`. |

#### `modules.checklist`

| Field | Type | Required | Notes |
|---|---|---|---|
| `entries` | map of entry id to entry | yes | A map, so two members ticking different entries both keep their tick |
| `entries.{id}.text` | string | yes | Not blank. An entry without text (left by a tick on an entry removed elsewhere) is skipped. |
| `entries.{id}.checked` | boolean | yes | |
| `entries.{id}.quantity` | integer | no | Greater than 0 |
| `entries.{id}.order` | integer | yes | Position in the list |

#### `modules.reminder`

| Field | Type | Required | Notes |
|---|---|---|---|
| `offsetsMinutes` | array of integer | yes | Minutes before the due moment, each 0 or more. Needs `modules.schedule`. Each phone schedules its own local notifications from it while the item is open and not assigned to another member. |

### `spaces/{spaceId}/completions/{completionId}`

Who completed an item and when, one document per completion, written in the batch that marks the item done. Append-only: reopening the item keeps it, and nobody edits or deletes one. One collection per space, so rotation and fairness (#49 to #51) read every completion of a space in one query.

| Field | Type | Required | Notes |
|---|---|---|---|
| `itemId` | string | yes | The item completed |
| `completedBy` | uid | yes | The caller, as on the item |
| `completedAt` | timestamp | yes | Server time |

The occurrence date of a recurring item comes with recurrence (#46).

### `spaces/{spaceId}/expenses/{expenseId}`

An entry of the ledger. Balances are derived from these on the device and never stored ([ADR 0003](../adr/0003-balances-on-device.md)). Amounts are in euro cents until the space has a currency (#111).

| Field | Type | Required | Notes |
|---|---|---|---|
| `spaceId` | string | yes | Copy of the parent id. Never changes. |
| `title` | string | yes | 1 to 120 characters after trimming |
| `paidBy` | uid | yes | Who paid. For a settlement, the debtor. |
| `split` | map of uid to integer | yes | Cents each member owes, each above 0: the only amount stored, and the total is its sum. Replaced whole on an edit. For a settlement, one entry: the creditor. |
| `date` | string | yes | `YYYY-MM-DD`, chosen by the user |
| `kind` | string | yes | `expense` or `settlement`. Never changes. Unknown reads as `expense`, because it still moves money. |
| `itemId` | string | no | The item whose Cost module made it (#58). Never changes. |
| `createdBy` | uid | yes | Never changes |
| `createdAt` | timestamp | yes | Server time, never changes |

A document without a required field, with an empty `split` or with a malformed `date` is skipped when read.

### `invitations/{code}`

| Field | Type | Required | Notes |
|---|---|---|---|
| `spaceId` | string | yes | The space it invites to |
| `createdBy` | uid | yes | An owner or admin of that space |
| `createdAt` | timestamp | yes | Server time |
| `expiresAt` | timestamp | yes | Seven days after creation (`INVITATION_VALIDITY`); the rules accept up to 30 |
| `revoked` | boolean | yes | `false` on creation. Revoking sets it to `true` for good. |

The code is eight characters from `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (no `I`, `O`, `0` or `1`), derived from a Firestore auto id. The link is `orbit://join/<code>`.

## Access API

Every read and write the app makes, by repository. Reads are live listeners unless marked otherwise. Writes return once applied locally ([offline and sync](../architecture/offline-and-sync.md#writes-apply-locally-first)).

### `FirebaseAuthRepository`

| Method | Path | Operation |
|---|---|---|
| `session` | `users/{uid}` | Listener on the signed-in user's document, shared by every collector |
| `register` | `users/{uid}` | `set(merge)`: `displayName`, `language`, `timeZone`, `createdAt` |
| `signIn` | `users/{uid}` | `set(merge)`: `language`, `timeZone` |
| `signInWithGoogle` | `users/{uid}` | `get`, then `set(merge)` of the full profile if it does not exist, or of `language`, `timeZone` if it does. A failed `get` writes nothing. |
| `updateDisplayName` | `spaces` where `memberIds` array-contains uid | One-off `get` |
| | `users/{uid}`, `spaces/{id}/members/{uid}` for each | One batch: `set(merge)` of `displayName` |
| `acceptAiConsent` | `users/{uid}` | `set(merge)`: `aiConsentAt` (server time) |

### `FirestoreSpaceRepository`

| Method | Path | Operation |
|---|---|---|
| `observeSpaces` | `spaces` where `memberIds` array-contains uid | Listener, restarted when the signed-in user changes |
| `observeMembers` | `spaces/{id}/members` | Listener, retried for a few seconds while refused |
| `createSpace` | `spaces/{id}` and `spaces/{id}/members/{uid}` | One batch: the space (`ownerId` the creator) and the owner's membership |
| `joinSpace` | `invitations/{code}`, then `spaces/{id}` and `spaces/{id}/members/{uid}` | A `get` of the invitation from the server, a check of the user's spaces, then one batch: `memberIds` gets the user (`arrayUnion`) and the member document names the invitation. Awaited, never queued offline. |
| `updateSpace` | `spaces/{id}` | `update` of only the fields that changed: `name`, `accent`, or both |
| `changeRole` | `spaces/{id}/members/{uid}` | `update` of `role` |
| `transferOwnership` | `spaces/{id}`, `spaces/{id}/members/{new owner}`, `spaces/{id}/members/{uid}` | One batch: `ownerId`, the new owner's `role: owner`, the caller's `role: admin` |
| `leaveSpace` | `spaces/{id}` and `spaces/{id}/members/{uid}` | One batch: `memberIds` loses the user (`arrayRemove`), `formerMembers.<uid>` keeps their name and their member document is deleted. Not for the owner. |
| `deleteSpace` | `spaces/{id}` | `delete` of the space document only: its members and items stay, unreadable (ADR 0004) |

### `FirestoreItemRepository`

| Method | Path | Operation |
|---|---|---|
| `observeItems` | `spaces/{id}/items` ordered by `createdAt` | Listener, retried for a few seconds while refused |
| `observeAgenda` | `spaces/{id}/items` where `status == open` and `modules.schedule.date <=` a date | Listener, retried the same way. `agendaOf` sorts the result into overdue, today and this week |
| `create` | `spaces/{id}/items/{itemId}` | `set` of the whole new item |
| `delete` | same | `delete`. The rules allow its creator, an admin or the owner |
| `rename` | same | `update` of `title` |
| `setModule` | same | `update` of `modules.<type>` (the whole module) |
| `removeModule` | same | `update`: delete `modules.<type>` |
| `setEntryChecked` | same | `update` of `modules.checklist.entries.<id>.checked` |
| `addEntry` | same | `update` of `modules.checklist.entries.<new id>` with text, checked, optional quantity and order |
| `updateEntry` | same | `update` of `modules.checklist.entries.<id>.text` and `.quantity` only |
| `removeEntry` | same | `update`: delete `modules.checklist.entries.<id>` |
| `observeCompletions` | `spaces/{id}/completions` where `itemId ==` id, ordered by `completedAt` desc, the latest 10 | Listener, retried the same way |
| `observeMemberCompletions` | `spaces/{id}/completions` where `completedBy ==` uid, ordered by `completedAt` desc, the latest 10 | Listener, retried the same way |
| `complete` | same item, and a new `spaces/{id}/completions/{completionId}` | One batch: `update` of `status: done`, `completedBy`, `completedAt`, and `set` of the completion |
| `reopen` | same | `update` of `status: open`, delete `completedBy` and `completedAt` |

No method saves a whole existing item: every update writes only its field paths, so concurrent edits merge.

### `FirestoreExpenseRepository`

| Method | Path | Operation |
|---|---|---|
| `observeExpenses` | `spaces/{id}/expenses` | Listener on the whole ledger, sorted in the app (latest `date`, then latest `createdAt`), retried for a few seconds while refused |
| `add` | `spaces/{id}/expenses/{expenseId}` | `set` of the whole new expense |
| `edit` | same | `update` of `title`, `paidBy`, `split` and `date` |
| `delete` | same | `delete`. Deleting a settlement undoes it. |

### `FirestoreInvitationRepository`

| Method | Path | Operation |
|---|---|---|
| `observeInvitations` | `invitations` where `spaceId ==` id | Listener, newest first in the app, retried while refused |
| `createInvitation` | `invitations/{code}` | `set` of the new invitation |
| `revokeInvitation` | `invitations/{code}` | `update` of `revoked: true` |

### On the device only

| What | Where |
|---|---|
| The space opened last | Multiplatform Settings, key `last_space_id` (`SettingsLastSpaceRepository`) |
| Scheduled local notifications | Android `SharedPreferences` file `notifications` |

## Queries and indexes

| Screen | Query | Index |
|---|---|---|
| Spaces list | `spaces` where `memberIds` array-contains uid | automatic |
| Members | `spaces/{id}/members` | automatic |
| Items | `spaces/{id}/items` ordered by `createdAt` | automatic |
| Ledger | `spaces/{id}/expenses` | automatic |
| Invitations (owner, admin) | `invitations` where `spaceId ==` id | automatic |
| Member page | `spaces/{id}/completions` where `completedBy ==` uid ordered by `completedAt` desc | composite: `completedBy` asc, `completedAt` desc, in `firebase/firestore.indexes.json` |
| Item history | `spaces/{id}/completions` where `itemId ==` id ordered by `completedAt` desc | composite: `itemId` asc, `completedAt` desc, in `firebase/firestore.indexes.json` |
| Agenda | `spaces/{id}/items` where `status == open` and `modules.schedule.date <=` end of week | composite: `status` asc, `modules.schedule.date` asc, in `firebase/firestore.indexes.json` |

## Coming next

Each extends this page in the PR that adds it: recurrence (#45) and the activity history (#83).
