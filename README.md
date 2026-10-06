# Orbit

Orbit is a shared-life organizer for any group that shares responsibilities: flatmates, families, couples, trips, sports teams, study groups. Each group is a **space** whose members share tasks, rotating chores, bills, lists, events and expenses with balances, synced in real time.

You don't fill in setup forms. A cloud AI assistant asks a few short questions and proposes a plan for the space. You review each proposal and confirm; nothing is saved without your confirmation. A receipt scanner turns a photo of a receipt into a split expense.

## Features
- Accounts, spaces and members
- Modular items: tasks, chores with rotation, bills, lists, events
- Agenda and calendar
- Expenses and balances
- Guided planning assistant (cloud AI)
- Receipt scanner

## Tech stack
Kotlin 2.4 · Compose Multiplatform 1.12 + Material 3 · Navigation Compose (type-safe routes) · AndroidX Lifecycle ViewModel · Koin · Firebase (Authentication, Firestore with offline persistence, Cloud Functions in Python) · kotlinx coroutines / serialization / datetime · Multiplatform Settings · Coil 3 · Kermit.
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
  core/navigation           Type-safe routes and OrbitNavHost
  domain/model              Plain Kotlin models
  domain/repository         Repository interfaces
  data/repository           Repository implementations
  feature/<name>            ui/, ViewModel and UiState per feature
  di                        Koin modules and initKoin()
shared/src/androidMain, iosMain   Platform actuals under the same packages
firebase/                   Firestore rules and Cloud Functions (Python)
```

## Contributing
Work is tracked on a GitHub Projects board with one milestone per sprint, and every change starts from an issue. See [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow and [AGENTS.md](AGENTS.md) for the full project rules.
