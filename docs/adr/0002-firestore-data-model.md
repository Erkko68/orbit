# ADR 0002: Firestore data model for users, spaces, memberships and items

- Status: Accepted
- Date: 2026-10-06
- Issue: #20

## Context

[ADR 0001](0001-local-persistence.md) made Firestore, with its offline cache, the only store. The cache cannot join or aggregate across collections, so the documents have to be shaped around the queries the screens make. This ADR fixes the Sprint 1 shape: users, spaces, memberships and items with the Schedule, Assignment, Checklist and Reminder modules. The Kotlin domain classes (#15, #21, #29), the repositories (#21, #30) and the security rules (#41) follow it.

## Decision

### Layout

```
users/{uid}                          private profile
spaces/{spaceId}                     the space, with the list of member ids
spaces/{spaceId}/members/{uid}       one membership: role and public profile copy
spaces/{spaceId}/items/{itemId}      one item: core fields and its modules
```

Everything a space owns is a subcollection of it, so "is this user a member of the space in the path" is the only access question the rules have to answer.

### Conventions

- Field names are camelCase. Enum values are lowercase strings. Optional fields are omitted, never stored as `null`.
- Ids: `users` and `members` documents use the Firebase Auth uid. Spaces and items use Firestore auto-ids, generated on the device so they work offline.
- Instants (`createdAt`, `completedAt`) are Firestore timestamps set with the server timestamp. While a write is still pending offline the server value is missing, so mappers read timestamps with the *estimate* behaviour.
- Calendar dates and times of day are strings (`2026-10-09`, `20:00`), read in the space's time zone. See "Schedule" below.
- A reference to another document is its id in a field ending in `Id` (`createdBy` and `completedBy` hold a uid). No Firestore `DocumentReference` fields.

### `users/{uid}`

Readable and writable only by its owner.

| Field | Type | Notes |
|---|---|---|
| `displayName` | string | |
| `photoUrl` | string, optional | |
| `language` | string | `es`, `ca` or `en` |
| `timeZone` | string | IANA id, e.g. `Europe/Madrid` |
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

### Queries and indexes

| Screen | Query | Index |
|---|---|---|
| Spaces home | `spaces` where `memberIds` array-contains uid | automatic |
| Member list | `spaces/{id}/members` | automatic |
| Item list | `spaces/{id}/items` ordered by `createdAt` | automatic |
| Agenda (today, this week) | `spaces/{id}/items` where `status == open` and `modules.schedule.date` <= end of week | composite: `status` asc, `modules.schedule.date` asc |

The agenda query has no lower bound on purpose: overdue items and windows that started earlier must still show, and the set of open items with a past date stays small. The app sorts the result into overdue, today and this week. Sorting spaces and members by name happens in the app. The composite index is added to `firebase/firestore.indexes.json` with the agenda query (#37).

### Access

The rules themselves are #41. The model assumes:

- `users/{uid}`: the owner only.
- A space and everything under it: members only.
- Items: any member creates, edits and completes.
- Space fields and other members' roles: `owner` and `admin`. A member can always edit the profile copy in their own member document and delete it to leave.

### Domain shape

A sketch of the Kotlin classes this maps to. The real code, with validation and tests, lands in #15, #21 and #29.

```kotlin
data class User(val id: String, val displayName: String, val photoUrl: String?, val language: String, val timeZone: TimeZone)

data class Space(val id: String, val name: String, val accent: Int, val memberIds: List<String>)

enum class Role { OWNER, ADMIN, MEMBER }
data class Member(val userId: String, val spaceId: String, val displayName: String, val photoUrl: String?, val role: Role)

enum class Preset { TASK, SHOPPING_LIST, EVENT }

sealed interface ItemStatus {
    data object Open : ItemStatus
    data class Done(val by: String, val at: Instant) : ItemStatus
}

/** An item as read from Firestore. */
data class Item(
    val id: String,
    val spaceId: String,
    val title: String,
    val description: String?,
    val status: ItemStatus,
    val preset: Preset?,
    val createdBy: String,
    val createdAt: Instant,
    val modules: ItemModules = ItemModules(),
)

/** What a person, a preset or the assistant fills in. The repository adds id, creator and timestamps. */
data class NewItem(
    val title: String,
    val description: String? = null,
    val preset: Preset? = null,
    val modules: ItemModules = ItemModules(),
)

enum class ModuleType { SCHEDULE, ASSIGNMENT, CHECKLIST, REMINDER }

sealed interface Module {
    val type: ModuleType
}

/** At most one module of each type, mirroring the `modules` map. */
data class ItemModules(
    val schedule: Schedule? = null,
    val assignment: Assignment? = null,
    val checklist: Checklist? = null,
    val reminder: Reminder? = null,
) {
    operator fun get(type: ModuleType): Module? = when (type) {
        ModuleType.SCHEDULE -> schedule
        ModuleType.ASSIGNMENT -> assignment
        ModuleType.CHECKLIST -> checklist
        ModuleType.REMINDER -> reminder
    }

    /** Present modules in card order. */
    val all: List<Module> get() = ModuleType.entries.mapNotNull(::get)

    fun with(module: Module): ItemModules = when (module) {
        is Schedule -> copy(schedule = module)
        is Assignment -> copy(assignment = module)
        is Checklist -> copy(checklist = module)
        is Reminder -> copy(reminder = module)
    }

    fun without(type: ModuleType): ItemModules = when (type) {
        ModuleType.SCHEDULE -> copy(schedule = null)
        ModuleType.ASSIGNMENT -> copy(assignment = null)
        ModuleType.CHECKLIST -> copy(checklist = null)
        ModuleType.REMINDER -> copy(reminder = null)
    }
}

data class Schedule(
    val date: LocalDate,
    val time: LocalTime? = null,
    val endDate: LocalDate? = null,
    val duration: Duration? = null,
) : Module {
    override val type get() = ModuleType.SCHEDULE
    val deadline: LocalDate get() = endDate ?: date
}

sealed interface Assignment : Module {
    override val type get() = ModuleType.ASSIGNMENT

    /** Who is responsible right now, whatever the mode. Backs the "mine" filter. */
    val assigneeId: String?

    data class ToMember(override val assigneeId: String) : Assignment
    data object Anyone : Assignment {
        override val assigneeId: String? get() = null
    }
    data class Claim(override val assigneeId: String? = null) : Assignment
}

data class Checklist(val entries: List<ChecklistEntry>) : Module {
    override val type get() = ModuleType.CHECKLIST
}
data class ChecklistEntry(val id: String, val text: String, val checked: Boolean = false, val quantity: Int? = null)

data class Reminder(val offsets: List<Duration>) : Module {
    override val type get() = ModuleType.REMINDER
}

sealed interface ItemError { /* BlankTitle, EndBeforeStart, ReminderWithoutSchedule, AssigneeNotMember, ... */ }
fun validate(title: String, modules: ItemModules, memberIds: Collection<String>): List<ItemError>
```

Why this shape:

- **`Item` is what is read, `NewItem` is what is written.** A person filling the create form, a preset and an assistant proposal all produce the same thing: a title and modules, with no id, creator or creation time yet (the server sets the time). A preset is a `NewItem` with default modules.
- **`ItemModules` holds one nullable property per module.** It mirrors the stored map, cannot hold the same module twice, and gives typed access (`item.modules.schedule?.date`) with no casts. It is its own class so `Item` and `NewItem` share it.
- **`ModuleType` exists because the screen needs the modules an item does *not* have** for its "Add detail" chips, and a sealed interface cannot list its subtypes in common code. `with` and `without` are the two module operations the assistant vocabulary is built on (§2.2).
- **Adding a module is compiler-guided.** A new `ModuleType` entry and `Module` class break every `when` above, plus the mapper and the module card, until each handles it. `all` is derived from `get`, so it cannot fall out of step.
- **`ItemStatus.Done` carries who and when,** so "done without a completer" cannot be built. It maps to `status`, `completedBy` and `completedAt`.
- **`Assignment.assigneeId` is on the interface** because lists and filters ask "whose is this now" without caring about the mode. Rotation (Sprint 2) becomes one more subtype that answers it.
- **Validation returns errors, it does not throw.** The classes can hold invalid values on purpose: a form needs to show what is wrong, and a malformed document or assistant proposal must not crash the app. Rules inside one module, across modules (a reminder needs a schedule) and against the space (assignee is a member) all come out of one `validate`, called before every write and on every proposal.
- **`Instant` and `Duration` are `kotlin.time`,** `LocalDate` and `LocalTime` are kotlinx-datetime. Checklist entries are a list in the domain, ordered by the stored `order`.

## Alternatives considered

- **Memberships as a top-level collection** (`memberships/{spaceId_uid}`). One query for each direction, but "my spaces" would return memberships and need one more read per space for its name and colour. Rejected for `memberIds` plus the subcollection.
- **Roles as a map on the space document.** One document instead of two, but no place for the profile copy, and every member list change rewrites the space.
- **Items as a top-level collection with `spaceId`.** Makes cross-space queries natural, but every rule would have to read the item to find its space. Kept the subcollection and the `spaceId` copy, which leaves a collection-group query open.
- **An `origin` field on the item** (`manual`, `template`, `ai`), as listed in §2.2 of the Sprint 0 document. Nothing reads it: no screen, query or rule depends on how an item was first created, and it says nothing about later edits. Labelling assistant changes (F15.1) and undoing a setup (F8.5) are per change, so they belong to the activity history when it is designed in Sprint 3.
- **One collection or subclass per item type** (task, list, event). Contradicts the modular model in §2.2 of the Sprint 0 document.
- **Timestamps for due dates.** Needs a time zone to answer "is it due today" and shifts all-day items for members in other zones.

## Consequences

- The item repository exposes operations (create, rename, set or remove a module, tick an entry, complete, reopen) and no `save(item)`. This is what the per-field merge and the forward compatibility above depend on, and it lines up with the assistant's vocabulary and the later activity history.
- `memberIds` and the member profile copies are duplicated data. They are only correct while every write that touches them goes through the repository batch that updates both.
- Joining from an invitation adds a user to a space they cannot yet write to. That write needs either a Cloud Function or a rule that checks the invitation; #24 decides it together with the invitation document.
- A checklist lives inside its item, and a document is limited to 1 MiB. Enough for thousands of entries; not a store for unbounded lists.
- Later modules (recurrence, cost, effort, guide, notes) add keys under `modules`. Expenses and the activity history become new subcollections of the space. Each extends this ADR in the PR that introduces it.
- Changing a field that is already in production data needs a migration, so schema changes are reviewed here first.
