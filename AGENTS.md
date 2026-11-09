# Orbit – agent & contributor rules

Orbit is a Kotlin Multiplatform (Android + iOS, Compose Multiplatform) organizer for groups that share responsibilities. A group is a *space* whose members share *items* (tasks, chores, bills, lists, events) and expenses, synced in real time through Firebase. A cloud AI assistant interviews the user and proposes a plan for the space. **Nothing is saved without user confirmation.**

This file holds the rules. The background to them (architecture, schema, roles, workflow, testing) is in [docs/](docs/README.md), and why a choice was made is in an [ADR](docs/adr/README.md).

## Architecture
- Modules: `shared/` (all app code), `androidApp/` (Android entry), `iosApp/` (Xcode entry), `firebase/` (backend), `server/` (the assistant).
- `firebase/`: everything deployed to Firebase. `firestore.rules`, `firebase.json` and the rules tests. Run the Firebase CLI from this folder.
- `server/`: the assistant's backend, a small Python HTTP server (standard library only) in Docker on our own server, next to its model ([ADR 0007](docs/adr/0007-assistant-on-our-own-server.md)).
- Package root: `com.orbit.app`. Code lives in `shared/src/commonMain` by default.
- `expect`/`actual` only where platforms really differ (camera, notifications, text recognition, file paths). Actuals go in `androidMain`/`iosMain` under the same package path.
- `domain/`: models, validation, repository **interfaces**, use cases. Pure Kotlin only (kotlinx coroutines/datetime allowed). No Compose, Room, Firebase or Koin. A computation that is a pure function of stored data (balances, the agenda's sorting) is a domain use case on the device, not server code ([ADR 0003](docs/adr/0003-balances-on-device.md)).
- `data/`: repository **implementations** and Firestore document mappers. Collection names, field readers and `requireUser` live once, in `data/repository/Firestore.kt`.
- `feature/<name>/`: `ui/` (screen + components), `<Name>ViewModel.kt`, `<Name>UiState.kt`. `feature/space` is the reference.
  - A component with its own ViewModel gets a subfolder, `feature/<name>/<component>/`, holding its composable, `<Component>ViewModel.kt` and `<Component>UiState.kt`. E.g. `feature/space/card/`, `feature/spacehome/items/`. See "State and ViewModels".
- UI talks only to ViewModels; ViewModels talk to repositories/use cases. UI never touches data sources.
- No placeholder folders: create a folder when its first file exists.
- No dev-only modes in the app (demo builds, flags that swap in fakes): the app always runs against Firebase. Fakes are for tests.
- Settings the phone already has (language, time zone, theme) are not app settings: read them from the device, never build a picker for them.

## State and ViewModels
- Screens and components hold no state. Every value they show or edit (field text, password visibility, selected tab or filter, loading, error) is a field of a `UiState` and changes only through a ViewModel function.
  - No `remember { mutableStateOf(…) }` or `rememberSaveable` for that in the UI. Only Compose's own holders stay there (scroll state, focus requesters, animation state).
  - A ViewModel exposes one `uiState: StateFlow<<Name>UiState>`. Repository flows are combined into it with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)`; what the user types or selects lives in a private `MutableStateFlow` combined with them.
  - The UI calls one ViewModel function per user action (`onEmailChange`, `selectTab`, `createSpace`) and passes no state back: the ViewModel reads what it needs from its own state.
  - Values derived from the state are properties of the `UiState` (`canCreate`), not expressions in the composable.
- Repositories: ViewModels depend on the repository interfaces in `domain/` only, never on an implementation, so the Firebase ones and the test fakes swap in Koin without touching a feature.
  - Repository reads and writes throw on failure: a repository never swallows one. Turning failures into state is the ViewModel's job, with the helpers in `core/` ([State and ViewModels](docs/architecture/state-and-viewmodels.md)).
  - Every read has a failure path and never crashes the app. A read the screen is about (a list, an item) goes through `asLoad`: it shows loading, then a failure with a retry (`loadFailed`, `retry()`). A read that only decorates (a name in a header, avatars) goes through `orOnFailure` and falls back to empty.
  - Every write goes through `attempt` in `viewModelScope` (auth calls through `executeAuth`), which logs the failure with Kermit. The failure is flagged in the `UiState` (`failed`, `createFailed`), cleared by the next edit or attempt.
  - Firebase repositories: a failed read is not "missing". Create a document only when the read says it does not exist, never after the read failed, or an existing one gets overwritten.
  - Firestore already shares one server listener between identical queries, so a repository flow needs no sharing of its own. Use `shareIn(…, replay = 1)` only when the flow does real work per collector, as `session` does (auth state plus the profile document).
  - A method only takes what its callers pass: no optional parameters for a caller that does not exist yet.
  - A screen that lands before its backend calls the real repository method; the Firebase implementation is a stub that logs with Kermit, returns, and names the issue that implements it.
  - A write in progress is a flag too (`submitting`, `creating`) and disables its button, so it cannot run twice.
- Component ViewModels: not one ViewModel per screen. A part of a screen that reads or writes a repository for one entity (a space card, the items tab, the member row) has its own ViewModel.
  - The screen ViewModel holds screen-level state and the **ids** of what the screen lists (`spaceIds`), never the loaded entities. The component gets the id, and its ViewModel loads and changes that entity.
  - The component is an `internal` composable in its own file that takes the id, a `modifier` and its ViewModel: `viewModel: XViewModel = koinViewModel(key = "<component>-$id") { parametersOf(id) }`, registered with `viewModelOf(::XViewModel)`. In a list, wrap it in `key(id) { … }`.
  - The key names the component as well as the id (`"items-$spaceId"`, `"members-$spaceId"`). Koin uses it as is, and the components of a screen share one ViewModel store: two keyed by the same id take one slot and keep replacing each other.
  - Keyed `koinViewModel`, not `rememberViewModelStoreOwner`: the ViewModel then lives as long as the screen's navigation entry, so a tab keeps its filter when the user switches tabs and comes back.
  - It follows every rule above: one `uiState`, repository interfaces only, failures become state.
  - Add one when the component does its own repository work. A part that only shows fields its parent already has stays a plain composable, and purely visual components (avatar, chip, button, list row) never get one: they take values and callbacks.
- Tests: every ViewModel has its own unit test, including the failure path of each read and each write. See [Testing](docs/process/testing.md).
  - A screen test whose components resolve their ViewModels calls `startTestKoin(…)`, and `stopKoin()` after.
  - Test doubles (the `InMemory…Repository` fakes, `FakeSpaceRepository`, `FlakySpaceRepository`, `FailingAuthRepository`, `startTestKoin`) live in `commonTest/…/testing/`, not next to the first test that needed them, and never in `commonMain`.
  - In a UI test, `performScrollTo()` before `performClick()`: the host test window is small and a click on something off screen does nothing.

## Libraries (use these, don't add alternatives)
- DI: Koin. Register in `di/Koin.kt` (`dataModule`, `featureModule`) or the `platformModule` actuals. Add a `domainModule` with the first use case that needs injecting. ViewModels: `viewModelOf(::X)` + `koinViewModel()` in the screen, or `koinViewModel(key = id) { parametersOf(id) }` in a component with its own ViewModel.
- Navigation: Navigation Compose with `@Serializable` routes in `core/navigation/Routes.kt`, wired in `OrbitNavHost`.
- Backend: Firebase (Authentication, Firestore), plus the assistant's own server (`server/`, ADR 0007). No other servers or REST APIs. The app calls the assistant server, never an AI provider directly.
- Local persistence: Firestore's offline cache only, no local database (Room, SQLDelight). Repositories read and write Firestore and work offline through its cache. See [ADR 0001](docs/adr/0001-local-persistence.md).
- Firestore schema: [docs/data/firestore-schema.md](docs/data/firestore-schema.md) is the source of truth for collections, fields, queries, writes and indexes, and [docs/data/security-rules.md](docs/data/security-rules.md) for roles and access. A schema or rules change updates those pages (and the rules tests) in the same PR; a new decision about the model gets an ADR. [ADR 0002](docs/adr/0002-firestore-data-model.md) records why the model has its shape.
- Roles: every member creates, edits and completes any item; owner and admin manage the space, its members and invitations, and delete any item; a member deletes only their own; only the owner changes roles, hands the space to an admin and deletes it. The UI checks `Role.managesSpace`, never a list of roles, and `firestore.rules` enforces the same table.
- Key-value preferences: Multiplatform Settings (`Settings` from Koin).
- Images: Coil 3. Logging: Kermit (no `println`). Dates: kotlinx-datetime. Serialization: kotlinx-serialization.
- UI tests: Compose `ui-test` in `commonTest`, so they run on Android (host, through Robolectric) and iOS. The test class extends `ComposeTest` and uses `runComposeUiTest` from `androidx.compose.ui.test.v2`. New screens and design-system components ship with one.
- Money: `Long` minor units (cents), never `Double`.

## AI
- The assistant runs on our own server, behind `server/` in Docker, next to its model: Gemma 4 on Ollama by default, Gemini's API by configuration ([ADR 0007](docs/adr/0007-assistant-on-our-own-server.md)). No on-device LLM.
- The server checks the caller's Firebase ID token on every request, and enforces the quota and the kill switch.
- AI output is always a proposal shown for review; only a user confirmation persists it.
- Provider keys live only in the server's environment (a `.env` next to `server/compose.yaml`), never in the repo or the app.
- Every assistant flow also has a manual path.

## UI and theme
- Wrap UI in `OrbitTheme`. Use `MaterialTheme.colorScheme` / `.typography` / `.shapes`, `OrbitTheme.colors` (success, warning, info, `spaceAccents`), `OrbitTheme.accent` (what the space's colour paints) and `OrbitTheme.data` (the type of amounts, times and dates).
- Material3 components, styled only through the theme. Shared wrappers go in `core/designsystem/component/`.
- Animations use `OrbitMotion.spatial()` / `OrbitMotion.effects()`.
- Material 3 Expressive, on stable material3: we follow its look through our own tokens and wrappers, not its alpha APIs. The why is in [docs/design/direction.md](docs/design/direction.md).
  - Colour: the space's accent is the page (`OrbitSpaceColor`, which sets `OrbitTheme.accent`). Panels stay neutral, so text never depends on the accent.
  - Depth: soft panels with no outlines, on the page. Everything keeps the gutter to the screen edges: only a top or bottom bar touches them. A panel is an `OrbitCard` or the rows of an `OrbitGroup`, and panels never nest.
  - Rows: what is listed sits in an `OrbitGroup`: small gaps, tight corners where rows meet, big ones at its ends, and a pressed row rounds off. Rows are `OrbitGroupRow`, `OrbitEntryRow`, `OrbitActionRow` or `OrbitTaskRow`. Every item, task or list, is an `OrbitTaskRow` in Tasks: things of the same kind are rows of one vertical list, never tiles, carousels or sideways scrollers.
  - Vivid fills are for what is touched every day: `OrbitColorHeader` on a screen about one thing, `OrbitSpaceTile`, and a balance (`OrbitAmountRow` with a sign: teal `credit` to get back, coral `debit` owed). `OrbitTurnCard` is the one hero moment: nothing else gets that treatment.
  - Navigation is the platform's ([ADR 0010](docs/adr/0010-standard-navigation-and-one-item-form.md)): a place the user goes (settings, a member, an item) is a route on the stack, with a back arrow, and system back and swipe-back do the same. Never a sheet with pages.
  - Headers show, the body edits: a colour header carries the title and actions, and what changes is a labelled row or field under it. The one exception is the title of an item or an expense, typed in place with its pencil.
  - One layout per screen: a screen keeps its sections in the same order whatever it holds, and an empty section shows its add row. An item is one form: title, Who, When, Reminder (once it has a date), one description, the checklist, the history, and one button at the bottom.
  - Still, not animated: motion is feedback (a press, a check, a page change). Nothing moves on its own.
  - One check: `OrbitCheck`, the same everywhere.
  - One add: `OrbitAddButton` floats in the same place on every screen. In a space it opens the item screen on a new item ([ADR 0007](docs/adr/0007-one-item.md)).
  - A screen with several groups opens each with an `OrbitSectionHeader`: the title and a count or an action.
  - Shape: cards `shapes.large`, controls `shapes.medium`, what is loud (a tile, the turn card) `shapes.extraLarge`, avatars and the add button circles. Badges and faces take a shape from `OrbitShape`.
  - Rhythm: buttons and text fields are 56dp tall (`ControlHeight`), and in a form they take the full width. Gaps are `md` inside a group and `lg` between groups. Never size a control at the call site.
  - Type: Jost for everything, big and bold: headlines bold, titles and labels semibold (set once in `Type.kt`). Martian Mono (`OrbitTheme.data`) for amounts, times and dates.
  - One primary action per screen. Everything else is a tonal or text action.
  - A short task over the screen (picking who has an item, a role, an invitation) is an `OrbitSheet`, not a form in the page. `OrbitDialog` is only for a prompt that stops the app, such as turning notifications on or confirming what cannot be undone (handing a space over). Whether either is open is a field of the `UiState`. Alternatives for the same goal share one sheet, split by an `OrbitLabeledDivider`.
  - Signed-out screens (sign-in, create account): `OrbitScreenHeader`, then one `OrbitCard` with the fields, their one filled button and the other ways in.
  - Every signed-in screen has a top: `OrbitTopBar` or `OrbitColorHeader`, through `OrbitScreen`. The spaces list shows the Orbit mark with the user's account avatar on the right.
  - Only a space has a bottom bar (`OrbitNavigationBar`, #239): Calendar, Tasks and Expenses, selected through `SpaceHomeUiState.tab`. The top bar title is the space's name, and tapping it opens the spaces list. Beside it the members' faces (`OrbitFaceStack`) and a settings button open the space's settings screen. The spaces list, the account and detail screens have no bar. A new section of a space goes in one of these destinations, not in a new tab; what opens from it is a pushed route.
  - No control for a feature that does not exist yet, unless the layout was agreed with it (e.g. joining with an invite code).
- No hardcoded colors: only `core/designsystem/theme/Color.kt` defines colors.
- Brand palette only: no dynamic (wallpaper) color.
- Contrast: every text/background pair must pass WCAG AA (4.5:1). Re-check before changing any scheme mapping.
  - `primary` is ink (`#0E1230`) in light and paper (`#F3F4F9`) in dark, on purpose: the accent belongs to the space, and text never depends on it.
  - Text on a vivid or partner fill is always `Ink`. Fills take no other text colour.
  - `spaceAccents` are decorative on the light canvas (under 3:1): a selected tab or a done row also changes ink or shows a tick, so colour never carries the meaning alone.
- Spacing and layout: styling decisions live in `core/designsystem/`, screens only compose.
  - Every padding and gap is an `OrbitSpacing` token (`xs` 4, `sm` 8, `md` 16, `lg` 24, `xl` 32). No raw `.dp` / `.sp` or `Color(0x…)` outside `core/designsystem/` (CI-checked in `ci.yml`).
  - Need a value that is not on the scale? Use the nearest token. Add a token only when the design really needs it, never a one-off number.
  - Every screen is built on `OrbitScreen`, which owns the scaffold, the insets, the screen gutter, the gap between sections and the scrolling. Screens never set their own outer padding or wrap their content in a `verticalScroll`: pass `scrollable = true`, or `list` for a lazy list.
  - Components have no outer margin: they pad their inside, take a `modifier` parameter, and the parent sets the gaps with `Arrangement.spacedBy(OrbitSpacing.…)`.
  - Typography by role (`titleMedium`, `bodyLarge`…), used as is. No `.copy(fontSize = …)` at the call site: if a role looks wrong, fix it in `Type.kt`.
  - The same styled element on a second screen becomes a wrapper in `core/designsystem/component/`. A screen that needs a visual tweak changes the wrapper or the theme, not itself.
  - Before writing a private composable in a feature, look in `core/designsystem/component/`. A title over a subtitle is `OrbitHeading`, the big opening of a screen `OrbitHero`, the opening of a screen without a top bar `OrbitScreenHeader`, rows `OrbitGroup` and its rows, "nothing here yet" `OrbitEmptyState`, an icon-only action `OrbitIconButton`, a password `OrbitPasswordField`, who has something `OrbitFaceStack`, an item in a list `OrbitTaskRow`, the destinations of a space `OrbitNavigationBar`, a filter `OrbitFilterChip`, a task over the screen `OrbitSheet`, the opening of a group `OrbitSectionHeader`, an icon on a shape `OrbitShapeBadge`, the row that adds to a group `OrbitAddRow`, the long text of something `OrbitDetailsField`, a length of time `OrbitTimeWheel`, a day `OrbitDatePicker`, an amount in a row `OrbitAmountField`, the colour of a space `OrbitSpaceColorPicker`.
  - A design-system component's icon parameter is a `DrawableResource`, and the component draws the `Icon`. A composable slot only when it can hold something else, such as an avatar.
  - A private composable stays in a feature only for a layout that belongs to that one screen (e.g. the schedule sheet of an item). Anything generic, such as text + icon + text, goes to the design system, takes plain values (`String`, not `StringResource`) and no ViewModel.
- No experimental Compose APIs (e.g. the Styles API, Material 3 Expressive `MotionScheme`). Revisit when stable.
  - Exceptions, each opted in where it is used and nowhere else:
    - Tests opt in to `ExperimentalTestApi`, because `runComposeUiTest` is the only multiplatform Compose test entry point.
    - M3's `DatePickerDialog`, which is stable on Android and only marked experimental on iOS. `TimePicker` stays out.
- No hardcoded strings in composables: use `composeResources/values/strings.xml`.
  - English is the default. A new or changed string ships in `values-es`, `values-ca` and `values-it` in the same change, with the same key, placeholders and place in the file as in `values/`. Android-only resources (`androidMain/res`) and iOS's `Info.plist` texts (`iosApp/iosApp/<lang>.lproj/InfoPlist.strings`) follow the same rule.
- One public composable per file.
- Immutable UI state (`data class` + `StateFlow`), no mutable collections in state.
- Kotlin official code style.

## Dependencies
- Add dependencies only to `gradle/libs.versions.toml`, never inline versions in build files.
- Latest stable versions only. No alpha/beta without team agreement.
- AGP stays on 9.1.x (and Gradle 9.5.x) so the project opens in IntelliJ IDEA.
- material3 stays on the stable line (1.9.x), even though newer alphas exist.

## Git workflow
- 4 members, GitHub Projects kanban with columns Backlog, Planned, In progress, Done.
- The board and its roadmap keep themselves up to date (`project-sync.yml`). How it works: [docs/process/project-automation.md](docs/process/project-automation.md). The walk-through of the whole workflow: [docs/process/workflow.md](docs/process/workflow.md).
  - Never edit Status, `Start date`, `Target date` or `Deviation (days)` by hand: the next run overwrites them.
  - What people maintain: the milestone, the "Blocked by" links, `Priority`, `Estimate` in hours (or at least a `size:` label), and `Actual`. The plan and the dashboard are computed from those.
  - Assign yourself the day you start, not when you finish: the assignment starts the issue's bar and the clock for its deviation.
- **Log the hours you really spent in the issue's `Actual` field (project board, hours, decimals allowed) before asking for review. Mandatory: the `Actual hours` check fails the PR until every issue it closes has one, and the project's cost and earned value are computed from it.**
  - The total for the issue, review fixes included: update it if more work lands before the merge.
  - From the terminal: `python3 .github/scripts/actual_hours.py --set <issue> <hours>`. Then re-run the check: editing a board field triggers nothing.
- Starting work. Agents do these steps, in order, before changing any code:
  1. Check the GitHub CLI with `gh auth status`. If `gh` is missing or not logged in, suggest setting it up in the terminal (`brew install gh` or https://cli.github.com, then `gh auth login`) before anything else.
  2. Find the current milestone (the open one with the earliest due date) and list its free issues: `gh issue list --milestone "<milestone>" --search "no:assignee"`. Ask the user which one to work on, following the tracks and order in [ROADMAP.md](ROADMAP.md). Skip issues whose "Blocked by" issues are still open.
  3. Assign it to the user with `gh issue edit <issue> --add-assignee @me` when the work starts, not when it is done: the assignment moves it to In progress and starts its bar on the roadmap.
  4. Create the branch for that issue (see below).
  5. Read the issue critically before building it. If a "Done when" item is wrong, outdated or does not make sense for the product (e.g. #19 asked for language and time zone pickers the phone already provides), or the issue misses something (an ADR rule, a platform), do not build it as written: fix the issue if the user owns it, otherwise comment on it proposing the change, and tell the user. Issues that depend on it get rescoped too.
- Finishing work. Before opening the PR, agents ask the user how many hours the issue took and log them with `python3 .github/scripts/actual_hours.py --set <issue> <hours>`. Never guess or estimate the number for them.
- An assigned issue is taken. Never pick, reassign or commit against an issue assigned to someone else unless the user explicitly says they are helping on it. In that case add the user as a second assignee and leave the original one.
- **Every commit references its issue as `(#<issue>)` at the end of the subject, and every PR closes it with `Closes #<issue>`. No issue, no commit: create the issue first.**
- Commits: Conventional Commits, imperative, subject ≤ 72 chars.
  - `feat(expenses): add balance calculation (#42)`
  - Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `build`.
- Commit body: 1–3 short lines on what and why, never how. No bullet walls.
- Always suggest small, focused commits, one logical change each. Never bundle unrelated changes.
- Branches: `<type>/<issue>-short-name`, lowercase, one branch per issue (CI-checked).
  - `<type>`: `feature` (for feat), `fix`, `refactor`, `docs`, `test`, `chore`, `build`.
  - e.g. `feature/42-balance-calculation`, `fix/57-rotation-skips-member`.
- PRs: one per complete functionality, not per commit.
  - Title: Conventional Commits **without** the issue number (GitHub appends the PR number on squash), e.g. `feat(expenses): add balance calculation`. CI-checked.
  - Body: what, `Closes #<issue>`, how to test, reviewer notes (`.github/pull_request_template.md`).
  - `Closes #<issue>` is what closes the issue on merge and moves it to Done. A bare `#<issue>` only links it.
  - `Closes` only when every "Done when" item is met, including "Verified on Android and iOS". If part is left (a stub, one platform), say so in the PR body and link a follow-up issue for it.

## Merge rules (`main` ruleset, enforced by GitHub)
- No direct pushes, force pushes or deletion of `main`. Every change goes through a PR, including docs.
- **1 approval** from another team member. New pushes dismiss existing approvals.
- All review conversations resolved.
- Required checks: `Android build + tests`, `iOS compile + tests` (`ci.yml`), `PR title`, `Branch name`, `Actual hours` (`pr-conventions.yml`), `SonarCloud Code Analysis` (quality gate).
- `sonarqube.yml` runs SonarQube Cloud on every PR (`sonar-project.properties`). A failed quality gate blocks the merge, so fix what it reports before asking for review.
  - Kotlin coverage comes from Kover: the workflow runs `./gradlew :shared:koverXmlReport` (Android host tests) and Sonar reads `shared/build/reports/kover/report.xml`. The gate needs 80% coverage on new code, so new logic ships with tests.
  - Python coverage comes from pytest-cov: the workflow runs `pytest --cov` in `server/` and Sonar reads its `coverage.xml`. New server code ships with pytest tests too.
  - Temporary: `sonar.coverage.exclusions` skips `.github/scripts/**` because the scripts have no coverage report yet.
  - The Firebase repositories call the live SDK and cannot run in host tests (repositories are faked).
- Squash merge only: the squash commit is the PR title + description. The branch is deleted after merge.
- The repo owner (admin) can bypass these rules. Team members and agents cannot.
- Agents: never push to `main` or merge, even when running with the owner's credentials, unless the owner explicitly asks for that specific push. Work on a correctly named branch and open a PR for humans to review.

## Before committing
- `./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` passes.
- Tests: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`.
- A change to `firebase/firestore.rules`: the rules tests pass (`npx firebase emulators:exec --only firestore "npm test"` from `firebase/`).
- A change to what a page in `docs/` describes updates that page.
- Changed files formatted with the IDE formatter (Kotlin official style).

## Do not
- Reformat unrelated files.
- Bump dependency versions unless asked.
- Create new top-level folders without asking.
- Commit secrets, keystores or API keys.
- Add AI attribution to commits or PRs (no `Co-Authored-By: Claude`, no "Generated with …" lines).
- Save user data from AI output without an explicit confirmation step.
