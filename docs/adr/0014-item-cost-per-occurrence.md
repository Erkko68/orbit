# ADR 0014: An item's cost charges its occurrence once

- Status: Proposed
- Date: 2026-11-12
- Issue: #58

## Context

Completing an item with a cost must create its expense atomically. A recurring item keeps its id while its schedule moves forward (ADR 0011). Reopening is a correction, not another purchase. Independent devices can complete the same occurrence, and Firestore queues batches offline.

## Decision

`modules.cost` stores positive `Long` cents, a payer and a set of current participants. It is an instruction for future completions, not a second copy of the ledger. `splitEqually` converts it to authoritative expense shares at completion. The expense uses the item's title and the local completion day and retains `itemId`.

One batch writes the item, its append-only completion and, when needed, the expense. A new expense shares the completion's id; the completion stores `expenseId`. A checklist's last tick or removal that finishes it is part of this batch too. Item and membership inputs are read from Firestore's local cache, already populated by the screen's listeners, so completing does not wait for a server read when offline. An unavailable cached input is a failure, never an empty item or membership list.

The item has `completionVersion` (missing means zero), incremented once per completion, and `lastCompletionId`. Rules check the old version against the new one and the matching new history document. Two stale batches cannot both complete a cost-bearing occurrence.

`costExpenseId` marks an occurrence already charged. Reopening keeps it, so completing again creates history but no new expense, even if the expense was subsequently deleted. Rolling forward clears the marker: the next occurrence can charge again. Ending a recurrence keeps the marker for its final occurrence.

Changing or removing the cost never edits existing expenses. Corrections to a past expense use the existing expense form. The reference remains after removing the cost so removing and re-adding it cannot charge the same occurrence again.

## Alternatives considered

- Add an expense through a second repository call: permits partial completion.
- Use only `itemId` as the expense id: overwrites different recurring purchases.
- Use a fresh id on every call without a version guard: duplicate taps or two devices can add multiple expenses.
- Use an online transaction: loses the existing offline batch behavior.
- Rewrite past expenses whenever the module changes: changes already recorded purchases.

## Consequences

Legacy items need no migration. A malformed cost blocks completion rather than silently dropping the charge. Removed participants must be corrected before a new charge. The batch is atomic on the server; a local queued write is not a server acknowledgement, and a later refusal still follows the existing Firestore synchronization behavior.

Checklist recurrence behavior is unchanged: finishing a checklist closes it; advancing it and clearing its entries is outside #58. The manual checklist-to-expense flow is #60. No assistant proposal schema or currency behavior changes.

The rules must be deployed separately before using this feature against production. An older app cannot complete a cost-bearing item without the required batch; it can still read it and edit unrelated fields.
