# Orbit – agent & contributor rules

Orbit is a Kotlin Multiplatform (Android + iOS, Compose Multiplatform) organizer for groups that share responsibilities. A group is a *space* whose members share *items* (tasks, chores, bills, lists, events) and expenses, synced in real time through Firebase. A cloud AI assistant interviews the user and proposes a plan for the space. **Nothing is saved without user confirmation.**

## Architecture
- Modules: `shared/` (all app code), `androidApp/` (Android entry), `iosApp/` (Xcode entry), `firebase/` (backend).
- `firebase/`: everything deployed to Firebase. `firestore.rules`, `firebase.json` and `functions/` (Cloud Functions in Python, `firebase-functions`). Run the Firebase CLI from this folder.
- Package root: `com.orbit.app`. Code lives in `shared/src/commonMain` by default.
- `expect`/`actual` only where platforms really differ (camera, notifications, text recognition, file paths). Actuals go in `androidMain`/`iosMain` under the same package path.
- `domain/`: models, repository **interfaces**, use cases. Pure Kotlin only (kotlinx coroutines/datetime allowed). No Compose, Room, Firebase or Koin.
- `data/`: repository **implementations** and Firestore document mappers.
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
- Repositories: ViewModels depend on the repository interfaces in `domain/` only, never on an implementation, so the in-memory and Firebase ones swap in Koin without touching a feature.
  - Repository writes throw on failure. The ViewModel launches them in `viewModelScope`, catches the failure (rethrowing `CancellationException`), logs it with Kermit and flags it in the `UiState` (`failed`, `createFailed`), cleared by the next edit or attempt. A failed write never crashes the app.
  - Firebase repositories: a failed read is not "missing". Create a document only when the read says it does not exist, never after the read failed, or an existing one gets overwritten.
  - A singleton repository's flow that several ViewModels collect is shared (`shareIn(…, replay = 1)`), so they reuse one Firestore listener instead of opening one each.
  - A method only takes what its callers pass: no optional parameters for a caller that does not exist yet.
  - A screen that lands before its backend calls the real repository method; the Firebase implementation is a stub that logs with Kermit, returns, and names the issue that implements it.
  - A write in progress is a flag too (`submitting`, `creating`) and disables its button, so it cannot run twice.
- Component ViewModels: not one ViewModel per screen. A part of a screen that reads or writes a repository for one entity (a space card, the items tab, the members tab) has its own ViewModel.
  - The screen ViewModel holds screen-level state and the **ids** of what the screen lists (`spaceIds`), never the loaded entities. The component gets the id, and its ViewModel loads and changes that entity.
  - The component is an `internal` composable in its own file that takes the id, a `modifier` and its ViewModel: `viewModel: XViewModel = koinViewModel(key = id) { parametersOf(id) }`, registered with `viewModelOf(::XViewModel)`. In a list, wrap it in `key(id) { … }`.
  - Keyed `koinViewModel`, not `rememberViewModelStoreOwner`: the ViewModel then lives as long as the screen's navigation entry, so a tab keeps its filter when the user switches tabs and comes back.
  - It follows every rule above: one `uiState`, repository interfaces only, failures become state.
  - Add one when the component does its own repository work. A part that only shows fields its parent already has stays a plain composable, and purely visual components (avatar, chip, button, list row) never get one: they take values and callbacks.
- Tests: every ViewModel has its own unit test, including the failure path of each write.
  - A screen test whose components resolve their ViewModels calls `startTestKoin(…)`, and `stopKoin()` after.
  - Test doubles shared by several tests (`FakeSpaceRepository`, `FailingAuthRepository`, `startTestKoin`) live in `commonTest/…/testing/`, not next to the first test that needed them.
  - In a UI test, `performScrollTo()` before `performClick()`: the host test window is small and a click on something off screen does nothing.

## Libraries (use these, don't add alternatives)
- DI: Koin. Register in `di/Koin.kt` (`dataModule`, `domainModule`, `featureModule`) or the `platformModule` actuals. ViewModels: `viewModelOf(::X)` + `koinViewModel()` in the screen, or `koinViewModel(key = id) { parametersOf(id) }` in a component with its own ViewModel.
- Navigation: Navigation Compose with `@Serializable` routes in `core/navigation/Routes.kt`, wired in `OrbitNavHost`.
- Backend: Firebase only (Authentication, Firestore, Cloud Functions). No other servers or REST APIs. The app calls Cloud Functions, never an AI provider directly.
- Local persistence: Firestore's offline cache only, no local database (Room, SQLDelight). Repositories read and write Firestore and work offline through its cache. See [ADR 0001](docs/adr/0001-local-persistence.md).
- Firestore schema: [ADR 0002](docs/adr/0002-firestore-data-model.md) is the source of truth for collections, fields and indexes. A schema change updates the ADR in the same PR.
- Key-value preferences: Multiplatform Settings (`Settings` from Koin).
- Images: Coil 3. Logging: Kermit (no `println`). Dates: kotlinx-datetime. Serialization: kotlinx-serialization.
- UI tests: Compose `ui-test` in `commonTest`, so they run on Android (host, through Robolectric) and iOS. The test class extends `ComposeTest` and uses `runComposeUiTest` from `androidx.compose.ui.test.v2`. New screens and design-system components ship with one.
- Money: `Long` minor units (cents), never `Double`.

## AI
- The assistant runs in the cloud, behind Cloud Functions in `firebase/functions/`. No on-device LLM.
- AI output is always a proposal shown for review; only a user confirmation persists it.
- Provider keys live only in the Cloud Functions secret manager, never in the repo or the app.
- Every assistant flow also has a manual path.

## UI and theme
- Wrap UI in `OrbitTheme`. Use `MaterialTheme.colorScheme` / `.typography` / `.shapes` and `OrbitTheme.colors` (success, warning, info, `spaceAccents`).
- Material3 components, styled only through the theme. Shared wrappers go in `core/designsystem/component/`.
- Animations use `OrbitMotion.spatial()` / `OrbitMotion.effects()`.
- Material 3 Expressive, on stable material3: we follow its look through our own tokens and wrappers, not its alpha APIs.
  - Depth, not flat: the main content of a screen sits on elevated cards (`OrbitCard(elevated = true)`: bright fill, soft shadow), secondary content and groups on tonal cards (the default), and the screen's create action floats in an `OrbitExtendedFab`. Never elevate everything: one level of emphasis per screen.
  - Shape: cards use `shapes.large`, text fields `shapes.medium`, buttons, chips and the FAB are full pills, avatars and icon badges circles.
  - Rhythm: buttons, text fields and the FAB are all 56dp tall (`ControlHeight`: the M3 text field and the M3 Expressive medium button), and in a form they all take the full width. Gaps are `md` inside a group and `lg` between groups. Never size a control at the call site.
  - Type: headlines are bold, titles and labels semibold (set once in `Type.kt`). A card's name is a `titleLarge`, the line about it a `labelMedium` above it.
  - One primary action per screen (the FAB or one filled button). Everything else is a tonal or text action.
  - A short task over the screen (adding a space: name a new one, or enter an invite code) is an `OrbitDialog`, not a form in the page. Whether it is open is a field of the `UiState`. Alternatives for the same goal share one dialog, split by an `OrbitLabeledDivider`, instead of one entry point each.
  - Signed-out screens (sign-in, create account) are one centered column on the canvas: `OrbitScreenHeader`, the fields and their one filled button, then the other ways in. No card around the form: every control has the same width and the groups are told apart by spacing and the labeled divider.
  - Every signed-in screen has a top bar (`OrbitTopBar` through `OrbitScreen`). The spaces list shows the Orbit mark and name with the user's account avatar on the right.
  - No control for a feature that does not exist yet, unless the layout was agreed with it (e.g. joining with an invite code).
- No hardcoded colors: only `core/designsystem/theme/Color.kt` defines colors.
- Brand palette only: no dynamic (wallpaper) color.
- Contrast: every text/background pair must pass WCAG AA (4.5:1). Re-check before changing any scheme mapping.
  - `primary` is a derived violet tone on purpose (`#6860F7` light, `#857EFF` dark). Don't replace it with raw `Violet`, which fails AA.
  - Dark `onPrimary` is navy on purpose.
  - Teal, coral and sun are fills, never text on light backgrounds; text on them is navy.
  - `spaceAccents` are decorative (avatars, dots, borders), no body text on them.
- Spacing and layout: styling decisions live in `core/designsystem/`, screens only compose.
  - Every padding and gap is an `OrbitSpacing` token (`xs` 4, `sm` 8, `md` 16, `lg` 24, `xl` 32). No raw `.dp` / `.sp` or `Color(0x…)` outside `core/designsystem/` (CI-checked in `ci.yml`).
  - Need a value that is not on the scale? Use the nearest token. Add a token only when the design really needs it, never a one-off number.
  - Every screen is built on `OrbitScreen`, which owns the scaffold, the insets, the screen gutter, the gap between sections and the scrolling. Screens never set their own outer padding or wrap their content in a `verticalScroll`: pass `scrollable = true` (leave it off only when the content scrolls itself, e.g. a lazy list).
  - Components have no outer margin: they pad their inside, take a `modifier` parameter, and the parent sets the gaps with `Arrangement.spacedBy(OrbitSpacing.…)`.
  - Typography by role (`titleMedium`, `bodyLarge`…), used as is. No `.copy(fontSize = …)` at the call site: if a role looks wrong, fix it in `Type.kt`.
  - The same styled element on a second screen becomes a wrapper in `core/designsystem/component/`. A screen that needs a visual tweak changes the wrapper or the theme, not itself.
  - Before writing a private composable in a feature, look in `core/designsystem/component/`. A title over a subtitle is `OrbitHeading`, the opening of a screen without a top bar `OrbitScreenHeader`, a row of a list `OrbitListItem` (an icon row on its own card is `OrbitListCard`), "nothing here yet" `OrbitEmptyState`, an icon-only action `OrbitIconButton`, a password `OrbitPasswordField`, a group's overlapping avatars `OrbitAvatarStack`, a task over the screen `OrbitDialog`.
  - A design-system component's icon parameter is a `DrawableResource`, and the component draws the `Icon`. A composable slot only when it can hold something else, such as an avatar.
  - A private composable stays in a feature only for a layout that belongs to that one screen (e.g. the space home summary). Anything generic, such as text + icon + text, goes to the design system, takes plain values (`String`, not `StringResource`) and no ViewModel.
- No experimental Compose APIs (e.g. the Styles API, Material 3 Expressive `MotionScheme`). Revisit when stable.
  - Only exception: tests opt in to `ExperimentalTestApi`, because `runComposeUiTest` is the only multiplatform Compose test entry point. Never in production code.
- No hardcoded strings in composables: use `composeResources/values/strings.xml`.
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
- The board and its roadmap keep themselves up to date (`project-sync.yml`). How it works: [docs/project-automation](docs/project-automation/README.md).
  - Never edit Status, `Start date`, `Target date` or `Deviation (days)` by hand: the next run overwrites them.
  - What people maintain: the milestone, the "Blocked by" links, `Priority`, and `Estimate` in hours (or at least a `size:` label). The plan is computed from those.
  - Assign yourself the day you start, not when you finish: the assignment starts the issue's bar and the clock for its deviation.
- Starting work. Agents do these steps, in order, before changing any code:
  1. Check the GitHub CLI with `gh auth status`. If `gh` is missing or not logged in, suggest setting it up in the terminal (`brew install gh` or https://cli.github.com, then `gh auth login`) before anything else.
  2. Find the current milestone (the open one with the earliest due date) and list its free issues: `gh issue list --milestone "<milestone>" --search "no:assignee"`. Ask the user which one to work on. Skip issues whose "Blocked by" issues are still open.
  3. Assign it to the user with `gh issue edit <issue> --add-assignee @me` when the work starts, not when it is done: the assignment moves it to In progress and starts its bar on the roadmap.
  4. Create the branch for that issue (see below).
  5. Read the issue critically before building it. If a "Done when" item is wrong, outdated or does not make sense for the product (e.g. #19 asked for language and time zone pickers the phone already provides), or the issue misses something (an ADR rule, a platform), do not build it as written: fix the issue if the user owns it, otherwise comment on it proposing the change, and tell the user. Issues that depend on it get rescoped too.
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
- Required checks: `Android build + tests`, `iOS compile + tests` (`ci.yml`), `PR title`, `Branch name` (`pr-conventions.yml`), `SonarCloud Code Analysis` (quality gate).
- `sonarqube.yml` runs SonarQube Cloud on every PR (`sonar-project.properties`). A failed quality gate blocks the merge, so fix what it reports before asking for review.
  - Kotlin coverage comes from Kover: the workflow runs `./gradlew :shared:koverXmlReport` (Android host tests) and Sonar reads `shared/build/reports/kover/report.xml`. The gate needs 80% coverage on new code, so new logic ships with tests.
  - Temporary: `sonar.coverage.exclusions` skips `.github/scripts/**` and `firebase/**` because the Python code and Firebase have no coverage report yet. Remove the `firebase/functions/**` exclusion once the functions have pytest coverage reported to Sonar.
  - The Firebase repositories call the live SDK and cannot run in host tests (repositories are faked).
- Squash merge only: the squash commit is the PR title + description. The branch is deleted after merge.
- The repo owner (admin) can bypass these rules. Team members and agents cannot.
- Agents: never push to `main` or merge, even when running with the owner's credentials, unless the owner explicitly asks for that specific push. Work on a correctly named branch and open a PR for humans to review.

## Before committing
- `./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` passes.
- Tests: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`.
- Changed files formatted with the IDE formatter (Kotlin official style).

## Do not
- Reformat unrelated files.
- Bump dependency versions unless asked.
- Create new top-level folders without asking.
- Commit secrets, keystores or API keys.
- Add AI attribution to commits or PRs (no `Co-Authored-By: Claude`, no "Generated with …" lines).
- Save user data from AI output without an explicit confirmation step.
