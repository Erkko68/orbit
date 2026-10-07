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
- UI talks only to ViewModels; ViewModels talk to repositories/use cases. UI never touches data sources.
- No placeholder folders: create a folder when its first file exists.

## Libraries (use these, don't add alternatives)
- DI: Koin. Register in `di/Koin.kt` (`dataModule`, `domainModule`, `featureModule`) or the `platformModule` actuals. ViewModels: `viewModelOf(::X)` + `koinViewModel()` in the screen.
- Navigation: Navigation Compose with `@Serializable` routes in `core/navigation/Routes.kt`, wired in `OrbitNavHost`.
- Backend: Firebase only (Authentication, Firestore, Cloud Functions). No other servers or REST APIs. The app calls Cloud Functions, never an AI provider directly.
- Local persistence: Firestore's offline cache only, no local database (Room, SQLDelight). Repositories read and write Firestore and work offline through its cache. See [ADR 0001](docs/adr/0001-local-persistence.md).
- Firestore schema: [ADR 0002](docs/adr/0002-firestore-data-model.md) is the source of truth for collections, fields and indexes. A schema change updates the ADR in the same PR.
- Key-value preferences: Multiplatform Settings (`Settings` from Koin).
- Images: Coil 3. Logging: Kermit (no `println`). Dates: kotlinx-datetime. Serialization: kotlinx-serialization.
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
- No hardcoded colors: only `core/designsystem/theme/Color.kt` defines colors.
- Brand palette only: no dynamic (wallpaper) color.
- Contrast: every text/background pair must pass WCAG AA (4.5:1). Re-check before changing any scheme mapping.
  - `primary` is a derived violet tone on purpose (`#6860F7` light, `#857EFF` dark). Don't replace it with raw `Violet`, which fails AA.
  - Dark `onPrimary` is navy on purpose.
  - Teal, coral and sun are fills, never text on light backgrounds; text on them is navy.
  - `spaceAccents` are decorative (avatars, dots, borders), no body text on them.
- No experimental Compose APIs (e.g. the Styles API, Material 3 Expressive `MotionScheme`). Revisit when stable.
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
- Starting work. Agents do these steps, in order, before changing any code:
  1. Check the GitHub CLI with `gh auth status`. If `gh` is missing or not logged in, suggest setting it up in the terminal (`brew install gh` or https://cli.github.com, then `gh auth login`) before anything else.
  2. Find the current milestone (the open one with the earliest due date) and list its free issues: `gh issue list --milestone "<milestone>" --search "no:assignee"`. Ask the user which one to work on. Skip issues whose "Blocked by" issues are still open.
  3. Assign it to the user with `gh issue edit <issue> --add-assignee @me` and remind them to move it to In progress on the board.
  4. Create the branch for that issue (see below).
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

## Merge rules (`main` ruleset, enforced by GitHub)
- No direct pushes, force pushes or deletion of `main`. Every change goes through a PR, including docs.
- **1 approval** from another team member. New pushes dismiss existing approvals.
- All review conversations resolved.
- Required checks: `Android build + tests`, `iOS compile + tests` (`ci.yml`), `PR title`, `Branch name` (`pr-conventions.yml`), `SonarCloud Code Analysis` (quality gate).
- `sonarqube.yml` runs SonarQube Cloud on every PR (`sonar-project.properties`). A failed quality gate blocks the merge, so fix what it reports before asking for review.
  - Kotlin coverage comes from Kover: the workflow runs `./gradlew :shared:koverXmlReport` (Android host tests) and Sonar reads `shared/build/reports/kover/report.xml`. The gate needs 80% coverage on new code, so new logic ships with tests.
  - Temporary: `sonar.coverage.exclusions` skips `.github/scripts/**` and `firebase/functions/**` because the Python code has no coverage report yet. Remove the `firebase/functions/**` exclusion once the functions have pytest coverage reported to Sonar.
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
