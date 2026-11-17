# ADR 0016: Effort is credited once per occurrence

- Status: Proposed
- Date: 2026-11-16
- Issue: #50

## Context

Effort is optional, from 1 to 5 points. The fairness indicator compares current members' completed points over
the last 30 days. An item may be reopened, its effort edited and the same occurrence completed again. Neither
changing the module nor reopening should rewrite the original contribution or earn the same points twice.

## Decision

- `modules.effort.points` is optional. Adding it starts at 1; existing items receive no implicit effort.
- Each completion stores `effortPoints` when the item has effort. `effortCredited: false` identifies a repeat
  completion of an already completed occurrence. The first contribution keeps its original points, actual
  completer and timestamp, regardless of later edits, reassignment, removal or reopening.
- An item's `effortRecorded` flag remembers that this occurrence already made its effort decision, including a
  completion without effort. Reopening preserves it; reopening an older completed item sets it too, without
  inventing points for its past. Advancing a recurring item clears it for the new occurrence, in the same batch
  as its schedule and completion history. This follows the existing per-occurrence cost lifecycle (ADR 0014).
- The device calculates fairness from completion snapshots and current member ids, never from current item
  modules. It skips reopenings, uncredited snapshots, legacy entries without points and people no longer in
  the space. Rejoining restores their still-recent contributions. History remains append-only.
- The time window is the inclusive instant interval from now minus 30 days to now. A visible indicator refreshes
  its time every minute. Exact integer point totals remain the calculation's output; only displayed percentages
  are rounded to whole numbers. With no points, the UI says there is no data instead of reporting unfairness.
- Firestore rules validate the effort range, the snapshot against the item before completion, and the credit
  flag against the occurrence's recorded state. An edit or reopening cannot clear that state. They leave the
  existing permissions for completing and reopening unchanged.

## Alternatives considered

Deriving effort from today's item module would rewrite past contributions after an edit or removal. Summing
every completion snapshot would reward reopening and recompleting the same occurrence. Storing fairness totals
on member documents would duplicate a result that changes with time and membership. The recorded flag and the
credit flag keep the original contribution identifiable without scanning all of an item's older history.

## Consequences

The item history's and member activity's latest-ten queries cannot supply fairness: a dedicated completion
listener reads the space since the beginning of the 30-day window, without a result limit. No fairness totals
are persisted. No database cleanup, retention policy, fair rotation (#51), or change to reopening permissions is
part of #50. Old history without snapshots earns no retrospective points. An older client cannot complete an
item with effort without supplying its snapshot; it must update to support the module.
