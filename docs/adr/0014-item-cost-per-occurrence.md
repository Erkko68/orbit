# ADR 0014: An item's cost is a planned expense, charged once per occurrence

- Status: Proposed
- Date: 2026-11-12
- Issues: #58, #300

## Context

An item can cost money: a weekly shop, the monthly rent. Nobody owes anything until it is done, so the cost is a plan on the item and the ledger gets an expense only when the item is completed. A recurring item keeps its id while its schedule moves forward (ADR 0011), and reopening an item corrects a mistake, it is not another purchase.

The expense screen already splits money every way the app supports: equally, custom amounts with automatic shares, percentages and weights (ADR 0013), and it keeps members who left. A second, smaller editor for the cost would narrow that and drift from it.

## Decision

**The cost is an expense not added yet, planned on the expense screen, and completing the item adds it once per occurrence.**

- `modules.cost` has the expense's shape: `paidBy`, `split` in cents and the optional `customAllocation`. The Cost row of the item opens the expense screen (`ExpenseRoute` with the item's id), without a title or a date: those are the item's title and the day it is completed. Save writes the module, and removing it deletes the module. On a new item the cost goes to the expense screen and back as text (`PlannedCost.kt`), through the item screen's `SavedStateHandle`, and is written with the item on Create.
- Completing copies the cost into a new expense with `itemId`, in the batch that writes the item and its completion. The item records it in `costExpenseId`. Reopening keeps it, so completing the same occurrence again adds nothing; rolling a recurring item forward deletes it, so the next occurrence is charged.
- The repository reads the item and the members from Firestore's cache, which the screen's listeners keep, so completing works offline. A cost that names someone who left throws instead of charging them, and the item screen blocks Mark done and says why.
- Changing or removing the cost never touches expenses already added. A wrong price is fixed on that expense, which links back to the item.
- The rules only check the cost's shape when it changes. They do not force the expense into the completion's batch: any member can edit or delete an expense afterwards, so such a rule would protect nothing.

## Alternatives considered

- **A cost editor of its own on the item** (#58 as first merged): equal splits only, a second copy of the expense form to keep in step.
- **A version counter on the item, with rules that require the expense in the completion's batch** (also as first merged): about 60 lines of rules, the feature failing until they were deployed, and no protection, for the reason above. Two phones completing the same occurrence offline at once can still add two expenses; the duplicate is deleted in the ledger.
- **Add the expense when the item is created:** people would owe for something not bought yet, and a recurring item could charge only once.
- **Create the item before planning its cost:** the item would exist before Create, against ADR 0010's "a new item keeps everything on the device until Create".

## Consequences

- Items need no migration: without `costExpenseId`, no occurrence is charged yet. A cost written by the first version of #58 (`cents`, `memberIds`) reads as a malformed module and is skipped; it is planned again from the Cost row.
- `ExpenseViewModel` has three modes: a ledger entry, an item's cost and a new item's cost. They share every rule of the form.
- #60 (add a shopping list as an expense) can open the same screen from the item.
