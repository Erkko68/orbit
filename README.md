# Orbit

Orbit is a shared-life organizer for any group that shares responsibilities: flatmates, families, couples, trips, sports teams, study groups. Each group is a **space** with tasks, recurring chores that rotate automatically, a shared calendar, live shared lists and expenses with balances.

You don't fill in setup forms. You describe your group in text, voice or a photo, and an on-device LLM (Gemma 4 E2B via LiteRT-LM on Android, an equivalent on iOS) proposes actions such as "create space", "add chore" or "add expense". You review and confirm them; nothing is saved without your confirmation. A receipt scanner (ML Kit OCR + the local model) turns a photo of a receipt into split expenses.

## Features
- Spaces and members
- Tasks and recurring chores with rotation
- Shared calendar
- Live shared lists
- Expenses and balances
- "Just tell it" setup assistant (on-device AI) *(planned)*
- Receipt scanner *(planned)*

## Tech stack
Kotlin 2.4 · Compose Multiplatform 1.12 + Material 3 · Navigation Compose (type-safe routes) · AndroidX Lifecycle ViewModel · Koin · Room KMP (bundled SQLite) · Ktor · kotlinx coroutines / serialization / datetime · Multiplatform Settings · Coil 3 · Kermit.
All versions are in [gradle/libs.versions.toml](gradle/libs.versions.toml).

## Run
- **Android:** `./gradlew :androidApp:assembleDebug`, or the `androidApp` run configuration in Android Studio / IntelliJ IDEA.
- **iOS:** open [iosApp/iosApp.xcodeproj](iosApp/iosApp.xcodeproj) in Xcode and run. It builds the `Shared` framework through Gradle.
- **Tests:** `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`

## Structure
```
androidApp/                 Android entry point (Application, Activity)
iosApp/                     Xcode project, SwiftUI entry point
shared/src/commonMain/kotlin/com/orbit/app/
  core/designsystem/theme   OrbitTheme: colors, type, shapes, motion
  core/designsystem/component  Shared composables (buttons, cards, empty states)
  core/navigation           Type-safe routes and OrbitNavHost
  core/common, core/util    Cross-cutting helpers and extensions
  domain/model              Plain Kotlin models (Space, Chore, Expense, SetupAction…)
  domain/repository         Repository interfaces
  domain/usecase            Business rules
  data/local                Room database, DAOs, entities
  data/remote               Ktor APIs and DTOs
  data/mapper               DTO/entity ↔ domain mapping
  data/repository           Repository implementations
  data/ai                   SetupAssistant and ReceiptScanner interfaces
  feature/<name>            ui/, ViewModel and UiState per feature
  di                        Koin modules and initKoin()
shared/src/androidMain, iosMain   Platform actuals under the same packages
```

## Team workflow
- GitHub Projects kanban; every commit relates to an issue.
- Conventional Commits: `feat(expenses): add balance calculation (#42)`. Small, focused commits.
- Branches `<type>/<issue>-name` (e.g. `feature/42-balances`). One PR per complete feature, 2 approvals + green CI, squash merge. No direct pushes to `main`.
- Full rules: [AGENTS.md](AGENTS.md) (also read by Claude via [CLAUDE.md](CLAUDE.md)).
