# Orbit – agent & contributor rules

Orbit is a Kotlin Multiplatform (Android + iOS, Compose Multiplatform) organizer for groups that share responsibilities. A group is a *space* with tasks, rotating chores, a shared calendar, live lists and expenses with balances. Users describe their group in text, voice or a photo; an on-device LLM proposes `SetupAction`s that the user reviews. **Nothing is saved without user confirmation.**

## Architecture
- Modules: `shared/` (all app code), `androidApp/` (Android entry), `iosApp/` (Xcode entry).
- Package root: `com.orbit.app`. Code lives in `shared/src/commonMain` by default.
- `expect`/`actual` only where platforms really differ (camera, notifications, local AI, file paths). Actuals go in `androidMain`/`iosMain` under the same package path.
- `domain/`: models, repository **interfaces**, use cases. Pure Kotlin only (kotlinx coroutines/datetime allowed). No Compose, Room, Ktor or Koin.
- `data/`: repository **implementations**, Room (`local/`), Ktor (`remote/`), mappers, AI engines (`ai/`).
- `feature/<name>/`: `ui/` (screen + components), `<Name>ViewModel.kt`, `<Name>UiState.kt`. `feature/space` is the reference.
- UI talks only to ViewModels; ViewModels talk to repositories/use cases. UI never touches data sources.
- Empty folders hold a `.gitkeep`. Delete it when the folder gets its first real file.

## Libraries (use these, don't add alternatives)
- DI: Koin. Register in `di/Koin.kt` (`dataModule`, `domainModule`, `featureModule`) or the `platformModule` actuals. ViewModels: `viewModelOf(::X)` + `koinViewModel()` in the screen.
- Navigation: Navigation Compose with `@Serializable` routes in `core/navigation/Routes.kt`, wired in `OrbitNavHost`.
- Networking: Ktor Client only. Inject the shared `HttpClient` from Koin; APIs in `data/remote/api`, DTOs in `data/remote/dto`.
- Database: Room KMP. Entities in `data/local/entity`, DAOs in `data/local/dao`, registered in `OrbitDatabase`. On schema change: bump `version`, add a migration, commit `shared/schemas/`.
- Key-value preferences: Multiplatform Settings (`Settings` from Koin).
- Images: Coil 3. Logging: Kermit (no `println`). Dates: kotlinx-datetime. Serialization: kotlinx-serialization.
- Money: `Long` minor units (cents), never `Double`.

## AI
- Interfaces only in `data/ai/` (`SetupAssistant`, `ReceiptScanner`). `NoopSetupAssistant` is bound until real engines exist.
- AI output is always a proposal (`SetupAction`, `ScannedReceipt`) shown for review; only a user confirmation persists it.
- AI/ML dependencies are commented out in `libs.versions.toml`; uncomment them there when implementing.

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
- 4 members, GitHub Projects kanban. Every commit relates to an issue.
- Commits: Conventional Commits, imperative, subject ≤ 72 chars, issue ref at the end.
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

## Merge rules (`main` ruleset, enforced by GitHub)
- No direct pushes, force pushes or deletion of `main`. Every change goes through a PR, including docs.
- **2 approvals** from other team members. New pushes dismiss existing approvals; the last push must be approved by someone else.
- All review conversations resolved.
- Required checks: `Android build + tests`, `iOS compile + tests` (`ci.yml`), `PR title`, `Branch name` (`pr-conventions.yml`).
- Squash merge only: the squash commit is the PR title + description. The branch is deleted after merge.
- Agents: never push to `main`. Work on a correctly named branch and open a PR for humans to review.

## Before committing
- `./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` passes.
- Tests: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`.
- Changed files formatted with the IDE formatter (Kotlin official style).

## Do not
- Reformat unrelated files.
- Bump dependency versions unless asked.
- Create new top-level folders without asking.
- Commit secrets, keystores, or AI model files (`*.litertlm`, `*.task`).
- Add AI attribution to commits or PRs (no `Co-Authored-By: Claude`, no "Generated with …" lines).
- Save user data from AI output without an explicit confirmation step.
