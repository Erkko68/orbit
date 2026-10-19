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

### iOS runs on the simulator only
The project has no paid Apple Developer account, so iOS is built and tested only on the simulator, which needs no signing. Apple will not register the bundle id `com.orbit.app` to our team (someone else owns it), and a free team cannot get an APNs key, so push notifications from Firebase Cloud Messaging do not reach iOS. Local notifications work, and a push can be simulated with `xcrun simctl push booted com.orbit.app <file>.apns`.

## Structure
```
androidApp/                 Android entry point (Application, Activity)
iosApp/                     Xcode project, SwiftUI entry point
shared/src/commonMain/kotlin/com/orbit/app/
  core/designsystem/theme   OrbitTheme: colors, type, shapes, motion
  core/navigation           Type-safe routes and OrbitNavHost
  core/designsystem/component  Orbit… components every screen is built from
  core                      Load and attempt: how ViewModels read and write
  domain/model              Plain Kotlin models and validation
  domain/repository         Repository interfaces
  data/repository           Firestore implementations
  feature/<name>            ui/, ViewModel and UiState per feature, a subfolder per component
  di                        Koin modules and initKoin()
shared/src/androidMain, iosMain   Platform actuals under the same packages
shared/src/commonTest       Unit and UI tests, with the shared fakes in testing/
firebase/                   Firestore rules and their tests, Cloud Functions (Python)
docs/                       Architecture, schema, roles, workflow and decisions
```

## Documentation
Start at [docs/](docs/README.md): the [architecture](docs/architecture/overview.md), the [Firestore schema](docs/data/firestore-schema.md), [security rules and roles](docs/data/security-rules.md), the [workflow](docs/process/workflow.md) and the [decision records](docs/adr/README.md).

## Contributing
Work is tracked on a GitHub Projects board with one milestone per sprint, and every change starts from an issue. See [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow and [AGENTS.md](AGENTS.md) for the full project rules.
