# ADR 0018: The activity history is an append-only collection written in the batch of each change

- Status: Proposed
- Date: 2026-11-27
- Issue: #83

## Context

Members want to see what changed while they were away, and what the assistant did apart from what people did (#83). Undo (#84) and conflict handling (#85) build on the same record: undo needs the values a change replaced, and conflicts need to show who overwrote which field.

Items already keep their completions in `spaces/{id}/completions`, written in the batch that completes or reopens the item. Item writes are narrow, one field, module or checklist entry at a time (ADR 0002), and expenses are edited as one update.

## Decision

**Every write to an item or an expense adds one document to `spaces/{spaceId}/activity` in the same batch.** The entry says who (`actorId`), when (`at`, server time), what (`action`, `target`, `targetId`, `title`) and, for an edit or a delete, `changes`: each stored field path the write changed, mapped to its previous value as JSON text.

- **Same batch, from the repositories.** `FirestoreItemRepository`, `FirestoreExpenseRepository` and `FirestorePlanRepository` add the entry next to the write (`logActivity`), so a change and its entry are saved together or not at all, offline included. ViewModels do not know the history exists.
- **Previous values from the cache.** Before an edit or a delete, the repository reads the document from the cache the screen's listener keeps, as completing already does, and encodes the fields the write touches with the document classes. A delete keeps every field but `createdAt`, so #84 can put the target back.
- **JSON text for previous values.** A previous value can be a string, a list or a whole module. JSON text gives one type for the map, rules that only need `changes is map`, and decoding with the same serializers the documents use.
- **Assistant changes are marked.** What a confirmed plan writes carries `source: 'assistant'` and one `planId` for the confirmation, so the screen labels it and #84 can undo the whole plan.
- **Append-only rules.** A member adds only their own entry, its target exists after the batch (or is gone for a delete), and nobody edits or deletes one.

## Alternatives considered

- **Previous values as native Firestore values.** Readable in the console, but the map would hold mixed types and the Kotlin SDK writes one serializer per field, so a mixed map needs a field-by-field update the append-only rules would refuse.
- **A Cloud Function that records changes.** It would see every write, including ones from old builds, but the project has no Functions (only Firebase and the assistant server), it would not work offline, and it cannot tell the assistant's writes apart.
- **Reusing `completions`.** It holds one kind of event per item and is read by fairness and rotation. Widening it would make those queries filter out most of what they read.

## Consequences

- Every item and expense write is now a batch of two documents, and edits, deletes and reopenings read the cache first. A write on an item the cache does not hold fails instead of being written blind; every screen that writes an item listens to it.
- Ticking checklist entries adds one entry each, so a shopping run fills the day's history. If that gets in the way, the screen can fold consecutive edits of the same target by the same member.
- The rules cannot force a write to carry its entry. Writes from outside the app, or from builds before #83, have none.
- History grows without bound. The screen reads the last 30 days; trimming older entries is left for later.
