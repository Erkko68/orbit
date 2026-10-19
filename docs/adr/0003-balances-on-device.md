# ADR 0003: Balances are derived on the device, in the domain

- Status: Accepted
- Date: 2026-10-19
- Issues: #53 (expense model and ledger), #56 (balances and debt simplification)

## Context

A space's members share expenses. Each one has a payer, a total and a split: how much of it each member owes. The balances screen (#57) shows what each member is owed or owes, and the fewest payments that settle it.

That computation could run in a Cloud Function, be stored in a document that is kept up to date, or run on each phone. The expense model itself (#53) is not designed yet; this record fixes where balances come from so that #53 and #56 build towards it.

## Decision

**Balances are a pure function of the ledger, computed on the device by a use case in `domain/`. They are never stored.**

Expenses in, one net amount per member out, with debt simplification on top. The use case reads the space's expenses as they are and derives the balances every time.

- **It is pure domain code.** Expenses in, amounts out, nothing else. That is what `domain/` is for, and #56's property tests run as host tests, which count towards the Kover and Sonar coverage. Python functions are still outside the coverage report.
- **It works offline.** Expenses arrive through the Firestore cache, so a new expense updates the balances at once with no connection. A callable function would fail offline and need a loading state just to show a number.
- **Nothing is stored.** A stored balances document goes stale, or races when two members add expenses offline, and would need a trigger function to keep it consistent. Deriving them avoids that server-side complexity.

### Two rules every implementation follows

1. **Rounding is deterministic.** Amounts are `Long` cents. When a split leaves cents over, they go to members in a fixed order (sorted by uid). Every device then computes exactly the same result.
2. **The payer is credited with the sum of the stored shares, not the stated total.** A bad expense document then cannot create or destroy money, and the balances of a space always sum to zero. Firestore rules cannot loop, so they cannot sum a split map of any size: the rules check membership and field types, and the domain checks that the shares add up.

## Alternatives considered

- **A callable Cloud Function that returns the balances.** Central and easy to change, but fails offline, adds latency to a number the user expects at once, and its tests sit outside the coverage report.
- **A balances document kept up to date by a trigger function.** Fast to read, but the trigger is server-side code to deploy and test, and the document is wrong between a write and the trigger, or after two offline writes race.
- **Updating a stored balance from the app on each expense.** No server code, but two members adding expenses offline overwrite each other's update: the per-field merge does not add numbers.

## Consequences

- The balances screen reads the whole ledger of the space. For a household that is small, and the cache holds it.
- If a space ever has so many expenses that reading the whole ledger hurts, add a periodic checkpoint document (balances up to a date) and sum only what came after it. A shared household will not get there, so nothing is built for it now.
- The expense schema (#53) needs: the payer, the split as a map of uid to cents, and the stated total for display only. Shares are the source of truth.
- The domain owns the invariant "balances sum to zero", and #56's property tests check it on random ledgers.
