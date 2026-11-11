# ADR 0013: Expense allocation intent is optional metadata beside authoritative cents

- Status: Proposed
- Date: 2026-11-10
- Issue: #283

## Context

Custom amounts and Percentages need manual shares plus automatic allocation of the remainder. The positive-only
cent split cannot distinguish manual from automatic people or retain selected zero shares. ADR 0003 keeps
that split as the only financial source; PR #282 provides the four-mode form and simple percentage conversion.

## Decision

Add one optional `customAllocation` map of UID to boolean: true is automatic, false manual. Its keys are the
selected people, including zero shares, and cover every debt UID. Write it with the complete split in the
existing add/edit operation; delete it when saving Equal or Weights. Store no percentage, preferred mode,
amount, balance or duplicate total. Validate metadata separately from monetary data.

Reopening uses saved cents as-is in Custom amounts with the saved selection/status. No allocation occurs
until an allocation-related action; title, payer and date changes do not redistribute shares. Legacy,
unreadable or unsupported metadata falls back to manual debt participants, never a missing expense.

Keep PR #282's simple mode conversion. Finite percentage precision may change cents after an explicit mode
change; exact round-trip preservation and a general rational conversion engine are outside #283. Automatic
percentage remainder is divided exactly before rounding cents, independently of approximate field labels.

## Alternatives considered

- Infer intent from the cent amounts: equal-looking amounts do not prove automatic intent; zeros disappear.
- Two arrays of selected/automatic UIDs: redundant membership consistency for information one map expresses.
- Store percentages, mode and exact ratios: unnecessary persistence and complexity beyond this issue.

## Consequences

Existing documents require no migration. Balances and settlements continue reading only `split`. Metadata
adds model, mapper, repository, validation and rules tests without a new service, database or dependency.
Older clients may preserve stale metadata on an edit because they do not write it; the reader validates
coverage/context and safely falls back to manual intent when inconsistent. Saved cents remain authoritative.
The metadata is not recovery of unsaved drafts across app restarts.
