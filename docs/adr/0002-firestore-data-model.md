# ADR 0002: Firestore data model for users, spaces, memberships, items and invitations

- Status: Accepted. Decision 2's "no `ownerId`" is superseded by [ADR 0004](0004-leaving-and-handing-over-a-space.md).
- Date: 2026-10-06, revised 2026-10-19
- Issues: #20, invitations #24, roles and field checks #215

## Context

[ADR 0001](0001-local-persistence.md) made Firestore, with its offline cache, the only store. The cache cannot join or aggregate across collections, so the documents have to be shaped around the queries the screens make, and every screen must work from the cache alone.

The fields, queries and writes as they are today are in the [Firestore schema](../data/firestore-schema.md) reference, and who may write what in [Security rules and roles](../data/security-rules.md). This record keeps the decisions behind them and what was rejected.

## Decision

### 1. Everything a space owns lives under it

```
users/{uid}
spaces/{spaceId}
spaces/{spaceId}/members/{uid}
spaces/{spaceId}/items/{itemId}
invitations/{code}
```

"Is this user a member of the space in the path" is then the only access question the rules answer for members and items. Invitations are the one exception (decision 6).

### 2. Membership is stored twice: `memberIds` and `members/{uid}`

`memberIds` on the space exists for one query and one rule. "My spaces" is `spaces where memberIds array-contains <uid>`, which returns the space documents themselves, ready to list offline. The read rule on a space is `request.auth.uid in resource.data.memberIds`, with no extra document read. Plans cap a space at 8 members, so the array stays small.

The `members` subcollection holds each role and a copy of the member's public profile (name, photo). The copy lets `users/{uid}` stay private, and lets names and avatars render offline without one read per member. There is no `ownerId`: the owner is the member whose role is `owner`.

Creating a space, joining, leaving and handing over ownership write the space and the member documents in one batch, and renaming a user updates their profile and every copy in one batch, so the two never drift.

### 3. An item is a title and a map of modules

There is no type per item kind. An item has core fields and a `modules` map keyed by module type; a module is present when its key is. A map, not an array, because Firestore merges concurrent writes per field path: one member changing `modules.schedule.date` while another ticks a checklist entry offline both survive, which is the "last write wins per field" behaviour the spec asks for (F11.3). Checklist entries are a map by id for the same reason.

This only holds if writes are as narrow as the change, so the repository never saves a whole item. It updates one core field, one whole module or one checklist entry by field path. Narrow writes also protect newer data: a build that does not know a module key, a preset or an assignment mode written by a newer build skips it when reading, and because it never rewrites `modules`, cannot delete it.

The preset (`task`, `shopping_list`, `event`) is only a label for icons and filters. Behaviour comes from the modules.

### 4. Dates are wall-clock strings

`2026-10-09` and `20:00`, not timestamps, because that is what people mean: "Friday at 20:00" stays Friday at 20:00 for every member whatever their phone's time zone. ISO dates sort as text, so the agenda is a plain range query on `modules.schedule.date`. The app converts to an instant only when it schedules a local reminder, in the phone's time zone. A space has no time zone of its own.

An item is due at a point (`date`, optionally `time`) or within a window (`date` to `endDate`), never both.

### 5. Roles guard the space, not the items

Every member creates, edits and completes any item: a shared space is shared. Owner and admin manage the space (its settings, members and invitations) and can delete any item; a member deletes only items they created; only the owner deletes the space. The full table is in [Security rules and roles](../data/security-rules.md#roles), and `Role.managesSpace` mirrors it in the domain.

The rules also check what the app cannot be trusted to: a new space has only its creator as member, items keep their creation fields and a valid title, and only the caller can be recorded as who completed an item.

### 6. Invitations are top-level documents whose id is the code

The person joining holds a code or a link, not the space id, and is not a member yet, so everything under the space is closed to them. A document read by id answers "what does this code open" with one `get`.

- **The code** is eight characters from 32 symbols (capitals without `I` and `O`, digits 2 to 9), so nothing is misread aloud or from a screenshot, with 40 bits of it. It is derived from a Firestore auto id, which the SDK draws from a secure generator on the device, so it works offline. The rules require exactly this shape. Only an owner or admin can list the invitations of their own space.
- **The link** is `orbit://join/<code>`. The share text writes the code out too, for apps that do not open the link. A link for any browser needs a domain with app links, and the app has none. #25 decided to keep the `orbit://` link, which both platforms open in the app, and to ship the code beside it for everywhere else; an https link is a follow-up for when there is a domain.
- **Revoking** sets `revoked` to `true` for good. The document stays, so a revoked link can say so instead of looking mistyped. An invitation lasts seven days.
- **Joining goes through a rule that checks the invitation**, not a Cloud Function (#25). The client sends the same batch as any membership change and names the code; the rule reads the invitation and accepts only a valid, unexpired invitation to this space that adds exactly the caller. This needs no deployed service, is tested with the other rules, and costs about three reads per join. If the join outgrows what rules express (rate limits, abuse handling), a callable `joinSpace(code)` replaces only that write.

### 7. The domain mirrors the stored shape

The Kotlin classes are in `shared/…/domain/model`; the code is the reference. Why they look the way they do:

- **`Item` is what is read, `NewItem` what is written.** A form, a preset and an assistant proposal all produce a title and modules, with no id, creator or creation time yet.
- **`ItemModules` has one nullable property per module.** It mirrors the stored map, cannot hold a module twice, and gives typed access (`item.modules.schedule?.date`).
- **`ModuleType` exists** because the screen needs the modules an item does *not* have, for its "Add detail" chips. `with` and `without` are the module operations the assistant vocabulary is built on.
- **Adding a module is compiler-guided:** a new `ModuleType` and `Module` break every `when`, the mapper and the module card until each handles it.
- **`ItemStatus.Done` carries who and when,** so "done without a completer" cannot be built.
- **`Assignment.assigneeId` is on the interface,** because lists and filters ask "whose is this now" whatever the mode.
- **Validation returns errors instead of throwing.** A form needs to show what is wrong, and a malformed document or proposal must not crash the app. `validateItem` runs before every write and on every proposal.
- **No ids the caller already has.** An `Item` or `Member` is always read within its space, so neither carries the space id.

## Alternatives considered

- **Memberships as a top-level collection** (`memberships/{spaceId_uid}`). One query each way, but "my spaces" would return memberships and need one more read per space for its name and colour.
- **Roles as a map on the space document.** One document instead of two, but no place for the profile copy, and every member change rewrites the space.
- **Items as a top-level collection with `spaceId`.** Cross-space queries become natural, but every rule would read the item to find its space. The subcollection with a `spaceId` copy keeps a collection-group query open.
- **One collection or class per item kind** (task, list, event). Contradicts the modular model of the Sprint 0 document (§2.2).
- **Timestamps for due dates.** Needs a time zone to answer "is it due today", and shifts all-day items for members in other zones.
- **An `origin` field on the item** (`manual`, `template`, `ai`). Nothing reads it, and it says nothing about later edits. Labelling assistant changes and undoing a setup are per change, so they belong to the activity history (#83).
- **Members edit only their own items.** Safer against mistakes, but a household's chores and lists are everyone's, and a claim or rotation would need the creator to act. The activity history and undo (#83, #84) are the answer to mistakes instead.
- **Invitations under the space** (`spaces/{id}/invitations/{code}`). Finding one would take a collection-group query that non-members may run, which also lets them enumerate every code.
- **Deleting an invitation to revoke it.** A revoked link would look like a mistyped one.
- **Invitations that never expire.** A link left in a chat would open the space for good.

## Consequences

- The item repository exposes operations (create, rename, set or remove a module, tick an entry, complete, reopen) and no `save(item)`. The per-field merge and forward compatibility depend on it, and it lines up with the assistant's vocabulary and the activity history.
- `memberIds` and the profile copies are duplicated data, correct only while every write that touches them goes through the batch that updates both.
- An invitation is a bearer capability: whoever has the code can read it and, with #25, join until it expires or is revoked.
- A checklist lives inside its item, and a document is limited to 1 MiB: enough for thousands of entries, not for unbounded lists.
- New modules (recurrence, cost, effort, guide, notes) add keys under `modules`; expenses and the activity history become subcollections of the space. Each updates the schema reference, and a new ADR records what it decides.
- Changing a field already in production data needs a migration, so schema changes are reviewed before they merge.
