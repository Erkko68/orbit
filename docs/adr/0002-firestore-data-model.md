# ADR 0002: Firestore data model for users, spaces, memberships and items

- Status: Accepted
- Date: 2026-10-06
- Issue: #20, invitations: #24, roles and field checks: #215

## Context

[ADR 0001](0001-local-persistence.md) made Firestore, with its offline cache, the only store. The cache cannot join or aggregate across collections, so the documents have to be shaped around the queries the screens make. This ADR fixes the shape of users, spaces, memberships, items with the Schedule, Assignment, Checklist and Reminder modules, and invitations. The domain classes in `shared/…/domain/model`, the Firestore repositories in `shared/…/data/repository` and `firebase/firestore.rules` follow it.

## Decision

### Layout

```
users/{uid}                          private profile
spaces/{spaceId}                     the space, with the list of member ids
spaces/{spaceId}/members/{uid}       one membership: role and public profile copy
spaces/{spaceId}/items/{itemId}      one item: core fields and its modules
invitations/{code}                   an invitation to one space, the id is the code
```

Everything a space owns is a subcollection of it, so "is this user a member of the space in the path" is the only access question the rules have to answer. Invitations are the one exception, for the reason given under their own heading.

### Conventions

- Field names are camelCase. Enum values are lowercase strings. Optional fields are omitted, never stored as `null`.
- Ids: `users` and `members` documents use the Firebase Auth uid. Spaces and items use Firestore auto-ids, generated on the device so they work offline.
- Instants (`createdAt`, `completedAt`) are Firestore timestamps set with the server timestamp. While a write is still pending offline the server value is missing, so mappers read timestamps with the *estimate* behaviour.
- Calendar dates and times of day are wall-clock strings (`2026-10-09`, `20:00`), read in the time zone of the phone showing them. A space has no time zone of its own. See "Schedule" below.
- A reference to another document is its id in a field ending in `Id` (`createdBy` and `completedBy` hold a uid). No Firestore `DocumentReference` fields.

### `users/{uid}`

Readable and writable only by its owner. `language` and `timeZone` are copied from the phone when the account is made and on every sign-in, for the server (push notices, the assistant). The app never reads them back: it uses the phone's settings, which the user already manages there.

| Field | Type | Notes |
|---|---|---|
| `displayName` | string | |
| `photoUrl` | string, optional | |
| `language` | string | `es`, `ca` or `en`, from the phone |
| `timeZone` | string | IANA id, e.g. `Europe/Madrid`, from the phone |
| `createdAt` | timestamp | |

### `spaces/{spaceId}`

| Field | Type | Notes |
|---|---|---|
| `name` | string | |
| `accent` | number | Index into `OrbitTheme.colors.spaceAccents`. Not a colour value: the palette lives in the app. |
| `memberIds` | array of uid | Kept in step with the `members` subcollection |
| `createdBy` | uid | |
| `createdAt` | timestamp | |

`memberIds` exists for one query and one rule. "My spaces" is `spaces where memberIds array-contains <uid>`, which returns the space documents themselves, ready to list offline. The read rule on a space is `request.auth.uid in resource.data.memberIds`, with no extra document read. Plans cap a space at 8 members, so the array stays small.

### `spaces/{spaceId}/members/{uid}`

| Field | Type | Notes |
|---|---|---|
| `role` | string | `owner`, `admin` or `member`. Exactly one `owner` per space. |
| `displayName` | string | Copy of the user's profile |
| `photoUrl` | string, optional | Copy of the user's profile |
| `joinedAt` | timestamp | |

The member list is one listener on this subcollection. The profile copy is what lets `users/{uid}` stay private and lets member names and avatars render offline without reading one user document per member. When a user edits their profile, the app updates their own member document in each of their spaces in the same batch.

There is no `ownerId` on the space: the owner is the member whose role is `owner`. Creating a space, leaving it and transferring ownership each write the space document and the member documents in one batch, so `memberIds` and the subcollection cannot drift.

### `spaces/{spaceId}/items/{itemId}`

Core fields:

| Field | Type | Notes |
|---|---|---|
| `spaceId` | string | Copy of the parent id, so a cross-space query stays possible later |
| `title` | string | 1 to 120 characters after trimming |
| `description` | string, optional | |
| `status` | string | `open` or `done` |
| `preset` | string, optional | `task`, `shopping_list` or `event` in Sprint 1. A label for the icon and filters; the modules are what define behaviour. |
| `createdBy` | uid | |
| `createdAt` | timestamp | |
| `completedBy` | uid, optional | Set together with `status: done`, removed on reopen |
| `completedAt` | timestamp, optional | Same |
| `modules` | map | One key per module present, see below |

Modules live in a map keyed by module type. A module is present when its key is, and removing a module deletes the key. A map, not an array, because Firestore merges concurrent writes per field path: one member changing `modules.schedule.date` while another ticks a checklist entry offline both survive, which is the "last write wins per field" behaviour the spec asks for (F11.3). An array would be overwritten whole.

This only holds if writes are as narrow as the change. The repository never saves a whole item back. It updates one core field, one whole module (`modules.schedule`) or one checklist entry (`modules.checklist.entries.{id}`) by field path. Narrow writes also protect newer data: a build that does not know a module key written by a newer build ignores it when reading and, because it never rewrites `modules`, cannot delete it. The same goes for an unknown `preset` (read as none) and an unknown `assignment.mode` (the module is skipped and logged).

**`modules.schedule`**

| Field | Type | Notes |
|---|---|---|
| `date` | string | `YYYY-MM-DD`. Due date, or first day of a flexible window. |
| `time` | string, optional | `HH:mm`. Absent means all day. Not allowed together with `endDate`. |
| `endDate` | string, optional | `YYYY-MM-DD`, after `date`. Present means "any time between `date` and `endDate`". |
| `durationMinutes` | number, optional | Greater than 0 |

Dates are wall-clock strings, not timestamps, because that is what users mean: "Friday at 20:00" stays Friday at 20:00 for every member whatever their device's time zone. ISO dates sort as text, so the agenda is a plain range query on `modules.schedule.date`. The app converts to an instant only when it schedules a local reminder.

An item is either due at a point (`date`, optionally `time`) or within a window (`date` to `endDate`), never both. Its *deadline* is `endDate` if present, otherwise `date`: an open item is overdue once the deadline is before today. Its *due moment*, used by reminders, is the deadline at `time`, or at 09:00 when there is no time.

**`modules.assignment`**

| Field | Type | Notes |
|---|---|---|
| `mode` | string | `member`, `anyone` or `claim` |
| `assigneeId` | uid, optional | Required for `member`. For `claim`, absent until a member claims it. Never present for `anyone`. |

Claiming is a transaction that fails if `assigneeId` is already set, so it needs connectivity. `assigneeId` must be in the space's `memberIds`.

**`modules.checklist`**

| Field | Type | Notes |
|---|---|---|
| `entries` | map of entry id to entry | |
| `entries.{id}.text` | string | Not blank |
| `entries.{id}.checked` | boolean | |
| `entries.{id}.quantity` | number, optional | Greater than 0 |
| `entries.{id}.order` | number | Position in the list |

Entries are a map for the same reason modules are: two members ticking different entries of a shopping list at the same time must not overwrite each other.

**`modules.reminder`**

| Field | Type | Notes |
|---|---|---|
| `offsetsMinutes` | array of number | Minutes before the due moment defined under Schedule, each 0 or more. One entry in Sprint 1. |

Requires `modules.schedule`. Reminders are local notifications: each device schedules its own from this field.

### `invitations/{code}`

An invitation to one space. The document id is the code the people being invited type or follow.

| Field | Type | Notes |
|---|---|---|
| `spaceId` | string | The space it invites to |
| `createdBy` | uid | An `owner` or `admin` of that space |
| `createdAt` | timestamp | Set by the server |
| `expiresAt` | timestamp | Seven days after creation. The rules accept up to 30. |
| `revoked` | boolean | `false` on creation. Revoking sets it to `true` and nothing sets it back. |

**Where it lives.** In a top-level collection, not under the space. The person joining holds a code or a link, not the space id, and is not a member yet, so everything under the space is closed to them. A document read by id answers "what does this code open" with one `get`. A subcollection would need a collection-group query by code, and letting non-members run it would also let them list every invitation.

**The code.** Eight characters from 32 symbols: the capital letters without `I` and `O`, and the digits 2 to 9. Nothing looks like anything else when it is read aloud or typed from a screenshot, and there are 40 bits of it. It is derived from a Firestore auto-id, which the SDK draws from a cryptographically secure generator on the device, so it works offline like the other ids. The rules require exactly this shape. Codes are only ever read by id. Only an `owner` or `admin` can list the invitations of their own space.

**The link.** `orbit://join/<code>`. The share text also writes the code out, so it still works where the message app does not open the link. A link that opens in any browser needs a domain with app links on both platforms, which is a decision for the issue that opens links in the app (#25).

**Revoking.** An `owner` or `admin` sets `revoked` to `true`. The document stays, so someone who opens a revoked link can be told it was revoked and not that it does not exist. An expired or revoked invitation is dead for good: a new one is a new code.

**Joining goes through a security rule, not a Cloud Function.** Joining writes to a space the person cannot write to yet: it adds their uid to `memberIds` and creates their `members/{uid}` document with the role `member`. Two ways to allow that:

- *A rule that checks the invitation* (chosen). The client sends the same batch it would send for any other membership change, and names the code in the member document. The rule reads the invitation with `get()` and accepts the batch only if the invitation exists, is not revoked, has not expired, belongs to this space, and the batch adds exactly the caller and nothing else. It can also keep a space at 8 members. This needs no new service: Sprint 1 has no deployed functions, a function needs the Blaze plan, and the first one (#43) is for the assistant. The rule is tested in the same emulator suite as the others, and the write is a plain Firestore batch like every other. Joining needs a connection either way, because the server has to look at the invitation. The price is rules that are harder to read than code and about three document reads per join.
- *A callable Cloud Function `joinSpace(code)`.* The checks and the member cap would be ordinary code. It would add a service to deploy, pay for and keep warm for the one write that happens once per person per space, and its tests would sit outside the rules suite. Rejected for now. If the join rule grows past what rules express well (rate limits, abuse handling), this is the way out and only the join write changes.

This issue (#24) adds the part for sharing: the collection, the rules for creating, reading and revoking, and the repository. The join rule and write come with #25, which builds on this decision.

### Queries and indexes

| Screen | Query | Index |
|---|---|---|
| Spaces home | `spaces` where `memberIds` array-contains uid | automatic |
| Member list | `spaces/{id}/members` | automatic |
| Item list | `spaces/{id}/items` ordered by `createdAt` | automatic |
| Invitations of a space (members tab, owner and admin) | `invitations` where `spaceId == id` | automatic |
| Agenda (today, this week) | `spaces/{id}/items` where `status == open` and `modules.schedule.date` <= end of week | composite: `status` asc, `modules.schedule.date` asc |

The agenda query has no lower bound on purpose: overdue items and windows that started earlier must still show, and the set of open items with a past date stays small. The app sorts the result into overdue, today and this week. Sorting spaces and members by name happens in the app. The composite index is added to `firebase/firestore.indexes.json` with the agenda query (#37).

### Roles and access

A space is shared, so its items are too: every member creates, edits and completes any item, as flatmates do with one shopping list. Roles only guard what changes the space itself.

| | Owner | Admin | Member |
|---|---|---|---|
| Read the space, its members and items | yes | yes | yes |
| Create, edit and complete any item | yes | yes | yes |
| Delete an item | any | any | only one they created |
| Edit the space (name, colour) | yes | yes | no |
| Create, list and revoke invitations | yes | yes | no |
| Add a member, or change a role between member and admin | yes | yes | no |
| Remove a member | anyone but the owner | plain members only | no |
| Hand ownership to someone else | yes (#27) | no | no |
| Edit their own profile copy, leave | yes (leaving needs a new owner first, #27) | yes | yes |
| Delete the space | yes | no | no |

`Role.managesSpace` in the domain is the owner-or-admin column, so screens hide what the rules would refuse. The child profile (#109) will be a fourth role with less than a member.

The rules also check the shape of what is written:

- `users/{uid}`: the owner only.
- A new space has its creator as its only member (`memberIds == [uid]`), a name, an integer accent and the server time as `createdAt`. Others join through an invitation (#25).
- A new item is `open`, has a title of 1 to 120 characters after trimming, the caller as `createdBy`, the server time as `createdAt` and the path's `spaceId`. No update changes `createdBy`, `createdAt` or `spaceId`, the title stays valid, `status` stays `open` or `done`, and `completedBy` can only be set to the caller.
- Invitations: created, listed for their own space and revoked by an `owner` or `admin`; read by id by any signed-in user who has the code.

### Domain shape

The Kotlin classes live in `shared/src/commonMain/kotlin/com/orbit/app/domain/model` (`Item.kt`, `ItemModules.kt`, `Space.kt`, `Member.kt`, `User.kt`, `Invitation.kt`) and validation in `ItemValidation.kt` (`validateItem`). The code is the reference; this section only records why it has this shape. The domain classes carry no ids their caller already has: an `Item` or a `Member` is always read within its space, so neither holds the space id.

Why this shape:

- **`Item` is what is read, `NewItem` is what is written.** A person filling the create form, a preset and an assistant proposal all produce the same thing: a title and modules, with no id, creator or creation time yet (the server sets the time). A preset is a `NewItem` with default modules.
- **`ItemModules` holds one nullable property per module.** It mirrors the stored map, cannot hold the same module twice, and gives typed access (`item.modules.schedule?.date`) with no casts. It is its own class so `Item` and `NewItem` share it.
- **`ModuleType` exists because the screen needs the modules an item does *not* have** for its "Add detail" chips, and a sealed interface cannot list its subtypes in common code. `with` and `without` are the two module operations the assistant vocabulary is built on (§2.2).
- **Adding a module is compiler-guided.** A new `ModuleType` entry and `Module` class break every `when` above, plus the mapper and the module card, until each handles it. `all` is derived from `get`, so it cannot fall out of step.
- **`ItemStatus.Done` carries who and when,** so "done without a completer" cannot be built. It maps to `status`, `completedBy` and `completedAt`.
- **`Assignment.assigneeId` is on the interface** because lists and filters ask "whose is this now" without caring about the mode. Rotation (Sprint 2) becomes one more subtype that answers it.
- **Validation returns errors, it does not throw.** The classes can hold invalid values on purpose: a form needs to show what is wrong, and a malformed document or assistant proposal must not crash the app. Rules inside one module, across modules (a reminder needs a schedule) and against the space (assignee is a member) all come out of one `validateItem`, called before every write and on every proposal.
- **`Instant` and `Duration` are `kotlin.time`,** `LocalDate` and `LocalTime` are kotlinx-datetime. Checklist entries are a list in the domain, ordered by the stored `order`.

## Alternatives considered

- **Memberships as a top-level collection** (`memberships/{spaceId_uid}`). One query for each direction, but "my spaces" would return memberships and need one more read per space for its name and colour. Rejected for `memberIds` plus the subcollection.
- **Roles as a map on the space document.** One document instead of two, but no place for the profile copy, and every member list change rewrites the space.
- **Items as a top-level collection with `spaceId`.** Makes cross-space queries natural, but every rule would have to read the item to find its space. Kept the subcollection and the `spaceId` copy, which leaves a collection-group query open.
- **An `origin` field on the item** (`manual`, `template`, `ai`), as listed in §2.2 of the Sprint 0 document. Nothing reads it: no screen, query or rule depends on how an item was first created, and it says nothing about later edits. Labelling assistant changes (F15.1) and undoing a setup (F8.5) are per change, so they belong to the activity history when it is designed in Sprint 3.
- **Invitations as a subcollection of the space** (`spaces/{id}/invitations/{code}`). Fits "everything a space owns is under it", but the code carries no space id and the person joining cannot read the space. Finding the invitation would take a collection-group query that non-members may run, which also lets them enumerate every code. Kept a top-level collection read by id.
- **Joining through a Cloud Function.** See the invitations section. A callable `joinSpace(code)` is the fallback if the join rule becomes too complex.
- **Deleting an invitation to revoke it.** A revoked link would then look like a mistyped one. A `revoked` flag lets the app say what happened, and keeps the record.
- **Invitations that never expire.** A link forwarded or left in a chat would open the space for good. Seven days, revocable, bounds it.
- **One collection or subclass per item type** (task, list, event). Contradicts the modular model in §2.2 of the Sprint 0 document.
- **Timestamps for due dates.** Needs a time zone to answer "is it due today" and shifts all-day items for members in other zones.

## Consequences

- The item repository exposes operations (create, rename, set or remove a module, tick an entry, complete, reopen) and no `save(item)`. This is what the per-field merge and the forward compatibility above depend on, and it lines up with the assistant's vocabulary and the later activity history.
- `memberIds` and the member profile copies are duplicated data. They are only correct while every write that touches them goes through the repository batch that updates both.
- Joining from an invitation adds a user to a space they cannot yet write to. It goes through a rule that checks the invitation, not a Cloud Function (see `invitations/{code}`). #25 adds that rule.
- An invitation is a bearer capability: anyone who has the code can read it, and can join until it expires or is revoked.
- A checklist lives inside its item, and a document is limited to 1 MiB. Enough for thousands of entries; not a store for unbounded lists.
- Later modules (recurrence, cost, effort, guide, notes) add keys under `modules`. Expenses and the activity history become new subcollections of the space. Each extends this ADR in the PR that introduces it.
- Changing a field that is already in production data needs a migration, so schema changes are reviewed here first.
