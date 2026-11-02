# Roadmap: rest of Sprint 2

Sprint 2 is due on **November 24, 2026**. New work stops on **November 17**, so that week is left for the usability test (#80), the two-device check (#237) and fixing what they find.

This page says what changed, who builds what and in which order. Dates, estimates and status live on the [project board](https://github.com/users/Erkko68/projects/3).

## What changed

- **A bottom bar inside a space** (#239). A space has three destinations: Home, Tasks and Expenses. The space's name is the top bar title, and tapping it opens the spaces list.
- **Foundations first.** Èric builds the shared base (repositories, navigation, design system, the first version of the new screens). The rest of the team builds on it.
- **A style of our own** (#241, #242). The screens feel clunky, so we research a direction before the new screens multiply the current one.
- **Issues were adjusted to match.** #38, #44, #54 and #57 are renamed, several have a "Decided in #238" section, and #78 moved to Sprint 3 with the rest of the scanner. Read that section before you start.

## How the app is organised

```
Spaces list (no bar)
  └── A space: top bar with its name, tap to go back to the list
        ├── Home      members, the assistant's entry point
        ├── Tasks     items and agenda (segmented tabs), month view
        └── Expenses  balances and ledger

Over the bar, which hides: an item, the expense form, the assistant
```

Everything in a space stays per space. There is no view across spaces in this sprint.

## Who builds what

Take any free issue in your track. Assign yourself the day you start.

| Track | Issues | Hours |
|---|---|---|
| **Foundations and UI** (Èric) | #238, #53, #239, #240, #241, #242, #54, #57, #71, #44, #72, #75, #76 | 59 |
| **AI** | #43, #64, #65, #66, #67, #68, #69, #70 | 44 |
| **Expense logic** | #56, #55, #58, #59, #60 | 17 |
| **Recurrence and chores** | #45, #46, #47, #48, #49, #50, #51, #52 | 31 |
| **Agenda** | #37, #38, #61 | 16 |
| **Assistant, on top of the base** | #74, #77, #63, #73 | 14 |
| **Everything else** | #62, #79, #102, #201, #202, #217, #188, #173, #81, #80, #237 | 41 |

## Order

**Now to November 8: the base**

1. #53: expense model and repository. This unblocks #54, #56, #58 and #60.
2. #239: the bottom bar. This unblocks #38, #44 and #54.
3. #240: item editors and module cards. This unblocks #46, #50, #58 and #75.
4. #241: style research, alongside the rest. Everyone is welcome to add references.
5. #43: the first Cloud Function. This unblocks all the AI work.

**November 9 to 17: the features**

- Foundations: #242, #54, #57, #71, #44, #72, #75, #76.
- AI: #64 first, because the screens depend on it, then #65 and #66.
- Everyone else: your track, in "Blocked by" order.

**November 17 to 24: check and fix**

#80, #237 and the defects they find. #73 (voice) only goes in if there is time.

## Working on top of the base

- **Agree the assistant contract on #64 first.** It sets the request, the response and the error codes that the screens use.
- **Do not wait for a screen or a backend.** A screen can land on a repository stub (AGENTS.md, "Repositories"). A PR on a stub links its issue without `Closes`.
- **Each module's card goes in its own file**, after #240.
- **Do not polish current screens** until #241 lands. Use the tokens and wrappers, so the restyle in #242 reaches your work on its own.
