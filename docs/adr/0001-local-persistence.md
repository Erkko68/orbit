# ADR 0001: Local persistence

- Status: Accepted
- Date: 2026-10-06
- Issue: #4

## Context

Orbit must stay usable without connectivity (F11.2) and sync spaces and items in real time through Firestore. The Sprint 0 scaffold shipped Room KMP as a local database, while the Sprint 0 document (§5.1) assumed Firestore's own offline persistence and left the choice open (§5.2). Everything in `data/` waits on this decision.

## Decision

Firestore's built-in offline persistence is the only local store for synced data. There is no local database: Room, KSP and the bundled SQLite driver are removed.

- Repositories read and write Firestore directly and expose snapshot listeners as `Flow`s. Reads are served from the cache when offline; writes are queued and sent on reconnect.
- Device-only key-value preferences (last opened space, theme) stay in Multiplatform Settings.
- `domain/` is unchanged: it only sees repository interfaces.

## Alternatives considered

**Room as the source of truth, Firestore synced into it.** Gives full SQL queries offline and independence from the Firestore SDK cache. Rejected: every model needs an entity, DAO, mapper and migration, plus hand-written two-way sync and conflict resolution that Firestore already provides. That cost is not affordable for a team of four over four sprints, and two stores are two places for the data to disagree.

## Consequences

- One source of truth and no sync code to write or test.
- Conflict resolution is Firestore's last-write-wins per field. Operations that need more (claiming a task, settling an expense) use transactions or Cloud Functions, which require connectivity.
- Offline queries are limited to what Firestore can do: no joins or cross-collection aggregation. The data model (#20) must be shaped around the screens' queries, with denormalised fields where needed.
- The cache only holds documents that were read while online and is evicted by size, so it is a cache and not a backup.
- Offline behaviour depends on the Firebase KMP SDK on both platforms; #6 validates it.
- If offline needs outgrow the cache (full-text search, heavy local aggregation), a local database can be added behind the existing repository interfaces without touching `domain/` or the UI. That would supersede this ADR.
