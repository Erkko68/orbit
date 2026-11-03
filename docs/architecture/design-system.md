# Design system

Orbit follows the look of Material 3 Expressive through its own tokens and wrappers on the stable `material3` line. Screens only compose: every styling decision lives in `shared/…/core/designsystem/`. The rules are in AGENTS.md ("UI and theme"); this page is the map.

## Theme

| File | Defines |
|---|---|
| `theme/Color.kt` | The palette, the light and dark schemes, `OrbitTheme.colors` (success, warning, info, `spaceAccents`) and `OrbitTheme.accent`, what a space's colour paints. The only file with color values. |
| `theme/Type.kt` | Type roles in Jost, headlines bold and titles and labels semibold, and `OrbitTheme.data` in Martian Mono for amounts, times and dates. |
| `theme/Shape.kt` | Cards `large`, controls `medium`, what is loud `extraLarge`. |
| `theme/OrbitShape.kt` | The expressive shapes (cookie, clover, squircle…) and the morph between them. |
| `theme/Spacing.kt` | `OrbitSpacing`: `xs` 4, `sm` 8, `md` 16, `lg` 24, `xl` 32 dp. Every padding and gap is one of these. |
| `theme/Motion.kt` | `OrbitMotion.spatial()` and `OrbitMotion.effects()` for animations. |
| `theme/OrbitTheme.kt` | `OrbitTheme { }`, which every piece of UI is wrapped in. Brand colors only, no wallpaper color. `OrbitSpaceColor` paints what it wraps in a space's colour. |

Every text and background pair passes WCAG AA (4.5:1) in both themes. `ComponentContrastTest` checks the pairs the components use.

## Depth

The space's colour is the page, and neutral panels sit on it with a shadow you only just see and no outline. Everything keeps the gutter to the screen edges, and only a top or bottom bar touches them. A panel is an `OrbitCard` or the rows of an `OrbitGroup`. Panels never nest. Vivid fills are for what is touched every day: a header, a list tile, a space tile, and the turn card as the one hero moment.

## Components

All in `core/designsystem/component/`, each with a UI test in `commonTest`. Before writing a private composable in a feature, look here.

### Structure

| Component | Use it for |
|---|---|
| `OrbitScreen` | Every screen: the page, scaffold, insets, gutter, gaps and scrolling (`scrollable = true`, or `list` for a lazy list) |
| `OrbitTopBar` | The top of a signed-in screen, on the page. The title can be a button, as a space's opens the list of spaces |
| `OrbitColorHeader`, `OrbitHeaderAction` | The top of a screen about one thing: a vivid block with its title, a summary and its one action as a pill |
| `OrbitHero` | The big bold opening of a screen, on the page |
| `OrbitScreenHeader` | The opening of a screen without a top bar (sign-in) |
| `OrbitSectionHeader` | The opening of a group: title, and a count or an action |
| `OrbitCard` | A panel |
| `OrbitGroup`, `orbitGroup` | Rows that read as one thing, in a column or a lazy list |
| `OrbitSheet` | A task over the screen. Whether it is open is `UiState`. |
| `OrbitDialog` | A prompt that stops the app, such as turning notifications on |
| `OrbitLabeledDivider` | "or" between two ways to the same goal |
| `OrbitStartup` | The logo on navy while the session is read |

### Content

| Component | Use it for |
|---|---|
| `OrbitHeading` | A title over a line of secondary text |
| `OrbitGroupRow` | One row of a group, with anything in it |
| `OrbitTaskCard` | A task as its own card: big title, summary, when and who, and the check |
| `OrbitEntryRow` | One entry of a checklist, lighter than a check row |
| `OrbitActionRow` | A row that does something: an icon on a shape, a title and an arrow |
| `OrbitNoteField` | Text of several lines typed where it is read |
| `OrbitDetailsField` | The long text of something, on a panel that takes the room left on the screen |
| `OrbitTimeWheel` | A length of time on two small wheels, 0 to 99 hours and minutes. Scroll one, or tap a number to type it. What is picked is `UiState`. It fits at the end of an `OrbitGroupRow` |
| `OrbitSpaceColorPicker` | The colour of a space, under its label |
| `OrbitListTile` | A list as a solid tile, with a peek at what is left |
| `OrbitSpaceTile` | A space in its own colour |
| `OrbitTurnCard` | The one hero moment: what is the user's to do now |
| `OrbitQuestionCard` | A question with answer chips (the assistant) |
| `OrbitAvatar` | A member's photo or initials |
| `OrbitFloatingFaces` | Who has something: faces that float and can be thrown |
| `OrbitShapeBadge` | An icon or the mark on an expressive shape |
| `OrbitMark` | The Orbit logo in the space's colour |
| `OrbitWavyProgress` | How far along something is |

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
| `OrbitTonalButton` | A secondary action, a sign-in provider |
| `OrbitTextButton` | A tertiary text action |
| `OrbitAddButton` | The one way to add something, floating in the same place on every screen |
| `OrbitIconButton` | An icon-only action, named for screen readers |
| `OrbitAvatarButton` | The account avatar in the top bar |
| `OrbitCheck` | The one check: a circle that morphs into a cookie when done |
| `OrbitTextField`, `OrbitPasswordField` | Text input. Visibility of a password is the caller's state. |
| `OrbitAddRow` | The row of a group that adds to it, typed in place |
| `OrbitFilterChip` | One option of a filter row |
| `OrbitColorPicker` | Picking a space's colour |
| `OrbitNavigationBar` | The destinations of a space, in `OrbitScreen`'s `bottomBar` ([ADR 0005](../adr/0005-bottom-bar-inside-a-space.md)) |

## Adding a component

1. The same styled element on a second screen becomes a component. A one-screen layout (the schedule sheet of an item) stays a private composable in its feature.
2. It takes plain values (`String`, not `StringResource`), a `modifier`, and callbacks. An icon parameter is a `DrawableResource` and the component draws the `Icon`.
3. It has no outer margin and no ViewModel. Its parent sets the gaps.
4. It uses only theme values and `OrbitSpacing`. Raw `dp`, `sp` and `Color(0x…)` outside `core/designsystem/` fail CI.
5. It ships with a UI test (`ComposeTest`, `runComposeUiTest`).

The look and why it is this way: [Visual direction](../design/direction.md).
