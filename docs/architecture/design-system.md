# Design system

Orbit follows the look of Material 3 Expressive through its own tokens and wrappers on the stable `material3` line. Screens only compose: every styling decision lives in `shared/…/core/designsystem/`. The rules are in AGENTS.md ("UI and theme"); this page is the map.

## Theme

| File | Defines |
|---|---|
| `theme/Color.kt` | The palette, the light and dark schemes, `OrbitTheme.colors` (success, warning, info, `spaceAccents`, `credit`, `debit`) and `OrbitTheme.accent`, what the accent paints. Three tonal palettes, a neutral, the brand blue and a coral, named by tone ([ADR 0012](../adr/0012-three-colours-and-a-flat-page.md)). The only file with color values. |
| `theme/Type.kt` | Type roles in Jost, headlines bold and titles and labels semibold, and `OrbitTheme.data` in Martian Mono for amounts, times and dates. |
| `theme/Shape.kt` | Cards `large`, controls `medium`, what is loud `extraLarge`. |
| `theme/OrbitShape.kt` | The expressive shapes (cookie, clover, squircle…) and the morph between them. |
| `theme/Spacing.kt` | `OrbitSpacing`: `xs` 4, `sm` 8, `md` 16, `lg` 24, `xl` 32 dp. Every padding and gap is one of these. |
| `theme/Motion.kt` | `OrbitMotion.spatial()` and `OrbitMotion.effects()` for animations. |
| `theme/OrbitTheme.kt` | `OrbitTheme { }`, which every piece of UI is wrapped in. Brand colors only, no wallpaper color. |

Every text and background pair passes WCAG AA (4.5:1) in both themes. `ComponentContrastTest` checks the pairs the components use.

## Depth

The page is one flat neutral, and panels sit on it one tone lighter, with a shadow you only just see and no outline; a field in a panel is a tone deeper. Everything keeps the gutter to the screen edges, and only a top or bottom bar touches them. A panel is an `OrbitCard` or the rows of an `OrbitGroup`. Panels never nest. Vivid fills are for what is touched every day: a header, a balance, and the turn card as the one hero moment.

## Components

All in `core/designsystem/component/`, each with a UI test in `commonTest`. Before writing a private composable in a feature, look here.

### Structure

| Component | Use it for |
|---|---|
| `OrbitScreen` | Every screen: the page, scaffold, insets, gutter, gaps and scrolling (`scrollable = true`, or `list` for a lazy list). `primaryAction` pins a form's one button at the bottom, `collapsingTopBar` slides the top away on scroll, `chat` grows a `list` up from the bottom |
| `OrbitTopBar` | The top of a signed-in screen, on the page. The title can be a button, as a space's opens the list of spaces |
| `OrbitColorHeader` | The top of a screen about one thing: a vivid block with its title, a face and a detail line. It shows; only an item's title is typed in it, with a pencil |
| `OrbitHero` | The big bold opening of a screen, on the page |
| `OrbitScreenHeader` | The opening of a screen without a top bar (sign-in) |
| `OrbitSectionHeader` | The opening of a group: title, and a count or an action |
| `OrbitCard` | A panel |
| `OrbitGroup`, `orbitGroup` | Rows that read as one thing, in a column or a lazy list |
| `OrbitSheet` | A short task over the screen. Whether it is open is `UiState`. A section of the app is a route, never a sheet |
| `OrbitDialog` | A prompt that stops the app, such as turning notifications on |
| `OrbitLabeledDivider` | "or" between two ways to the same goal |
| `OrbitStartup` | The logo on navy while the session is read |

### Content

| Component | Use it for |
|---|---|
| `OrbitHeading` | A title over a line of secondary text |
| `OrbitGroupRow` | One row of a group, with anything in it |
| `OrbitTaskRow` | An item in a list, a task or a list alike: the check, the title, a line of description, when and how far along, and the faces at the end |
| `OrbitEntryRow` | One entry of a checklist, lighter than a check row |
| `OrbitAmountRow` | A row about money: a title over a line, the amount at the end in Martian Mono, a face in front when it is a person. A ledger entry, or a balance: with a + or − sign the whole row is the vivid `credit` (teal) or `debit` (coral) fill; `done` tints a reimbursement |
| `OrbitAmountField` | An amount typed at the end of a row, such as one person's share: right-aligned in Martian Mono |
| `OrbitTransferRow` | A payment to settle: the two faces, the sentence ("You owe Ana") over its amount, and, when the user is the one who pays, a small teal pill with an icon ("Pay") that asks to record it |
| `OrbitActionRow` | A row that does something: an icon on a shape, a title over its value, faces or another `trailing`, and an arrow |
| `OrbitNoteField` | Text of several lines typed where it is read |
| `OrbitDetailsField` | The long text of something, on a panel as tall as its text |
| `OrbitDatePicker` | A calendar on a page of an `OrbitSheet`, with the button that confirms the day. The schedule of an item and the date of an expense use it |
| `OrbitTimeWheel` | A length of time on two small wheels, 0 to 99 hours and minutes. Scroll one, or tap a number to type it. What is picked is `UiState`. It fits at the end of an `OrbitGroupRow` |
| `OrbitSpaceTile` | A space as a panel led by the faces of the people in it, or its initials on a shape while it is only the user's |
| `OrbitMoneyField` | The amount a form is about, such as an expense's total: its own panel with a big number in the data type |
| `OrbitCodeTiles` | A short code, one character per tile, coming in tile by tile when it is made |
| `OrbitTurnCard` | The one hero moment: what is the user's to do now |
| `OrbitChatBubble` | One message of a chat (the assistant): the other side's on a panel at the left, the user's in blue at the right. Takes what answers it, such as chips, and a spinner while it is written |
| `OrbitChatComposer` | The bottom of a chat, in `OrbitScreen`'s `bottomBar`: a field and a round send button, above the keyboard |
| `OrbitAvatar` | A member's photo or initials |
| `OrbitFaceStack` | Who has something: up to four still faces and "+N". Tappable as one button, as in a space's top bar |
| `OrbitShapeBadge` | An icon or the mark on an expressive shape |
| `OrbitMark` | The Orbit logo in the accent |

### States

| Component | Shows |
|---|---|
| `OrbitLoadingState` | A list or tab that is loading |
| `OrbitErrorState` | A list or tab that failed to load, with "Try again" |
| `OrbitEmptyState` | A list or tab with nothing in it yet |
| `OrbitErrorText` | Why the last action failed, under what it was about. Announced by screen readers. |

### Controls

Buttons and text fields are 56 dp tall (`ControlHeight`) and take the full width in a form. One primary action per screen.

| Component | Use it for |
|---|---|
| `OrbitButton` | The one primary action |
| `OrbitTonalButton` | A secondary action on a blue container, a sign-in provider; shared rows can supply a shape and a compact label/padding variant while retaining the control height and theme colours |
| `OrbitTextButton` | A tertiary text action |
| `OrbitAddButton` | The one way to add something, floating in the same place on every screen |
| `OrbitIconButton` | An icon-only action, named for screen readers |
| `OrbitAvatarButton` | The account avatar in the top bar |
| `OrbitCheck` | The one check: a circle that morphs into a cookie when done |
| `OrbitTextField`, `OrbitPasswordField` | Text input. Visibility of a password is the caller's state. |
| `OrbitAddRow` | The row of a group that adds to it, typed in place |
| `OrbitFilterChip` | One option of a filter row |
| `OrbitNavigationBar` | The destinations of a space, in `OrbitScreen`'s `bottomBar` ([ADR 0005](../adr/0005-bottom-bar-inside-a-space.md)) |

## Adding a component

1. The same styled element on a second screen becomes a component. A one-screen layout (the schedule sheet of an item) stays a private composable in its feature.
2. It takes plain values (`String`, not `StringResource`), a `modifier`, and callbacks. An icon parameter is a `DrawableResource` and the component draws the `Icon`.
3. It has no outer margin and no ViewModel. Its parent sets the gaps.
4. It uses only theme values and `OrbitSpacing`. Raw `dp`, `sp` and `Color(0x…)` outside `core/designsystem/` fail CI.
5. It ships with a UI test (`ComposeTest`, `runComposeUiTest`).

The look and why it is this way: [Visual direction](../design/direction.md).
