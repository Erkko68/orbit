# ADR 0005: The bottom bar lives inside a space, and the title switches spaces

- Status: Accepted
- Date: 2026-11-02
- Issues: #238 (roadmap), #239 (the bar)

## Context

Sprint 2 adds expenses and an agenda to a space. Segmented tabs under the member row do not scale to that, and a space's home had grown into a list with a back arrow on top. The team wanted a bottom bar, and had to decide whether its destinations belong to the app or to one space.

## Decision

**Only a space has a bottom bar: Calendar, Tasks and Expenses. The top bar's title is the space's name, and tapping it opens the spaces list.**

- **Everything stays per space.** Items, the agenda and the ledger are read one space at a time, as ADR 0002 and ADR 0003 define them. No destination needs a query across spaces.
- **The bar is state, not navigation.** The selected destination is `SpaceHomeUiState.tab`, inside one route. The space's component ViewModels live with that route, so a destination keeps its filter when the user comes back.
- **Detail screens cover the bar.** An item, the expense form and the assistant are routes on top of the space.
- **The assistant is an action, not a destination.** A setup interview is something a user does now and then, and the one primary action of a screen stays its FAB.

## Alternatives considered

- **A bar for the whole app (Spaces, Tasks, Expenses).** One tap to everything due, but Tasks and Expenses would have to merge every space. That means a listener per space, and balances that cannot be added across spaces once each has its own currency (#111). It also repeats the same lists inside a space.
- **Keep the segmented tabs.** Nothing to build, but three sections plus the agenda inside Tasks would nest tabs in tabs.
- **A nested navigation graph per destination.** Each destination would get its own back stack, which nothing here needs: detail screens already sit on top of the space.

## Consequences

- There is no view of everything across spaces. Someone in two spaces switches to see each one. It can come later as its own screen.
- A new section of a space goes in one of the three destinations, not in a new tab or route.
- With no back arrow, iOS relies on the title and the swipe to return to the spaces list, so the title must read as a button.
