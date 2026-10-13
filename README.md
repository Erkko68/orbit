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

### Run on an iPhone
The simulator needs no setup. A physical iPhone needs your Apple team to sign the build:

1. You need Xcode 26, an iPhone on iOS 18.2 or later, and your Apple ID added in Xcode (Settings > Accounts). A free personal team is enough.
2. Create `iosApp/Configuration/Local.xcconfig` with one line, `TEAM_ID=XXXXXXXXXX`. Your team id is under Xcode > Settings > Accounts, or the "Organizational Unit" of your "Apple Development" certificate in Keychain Access. Git ignores this file: never put the id in `Config.xcconfig`.
3. Connect the iPhone by cable, unlock it and tap Trust. Turn on Developer Mode (Settings > Privacy & Security > Developer Mode) and restart it when asked.
4. Open [iosApp/iosApp.xcodeproj](iosApp/iosApp.xcodeproj), pick the iPhone as the destination and run. Signing is automatic: Xcode creates the profile on the first run.
5. With a free team, the first launch is blocked until you trust yourself on the phone (Settings > General > VPN & Device Management), and the install expires after 7 days.

On the simulator the bundle id is `com.orbit.app`, the one Firebase and its API key allow. Apple will not register that id to our team (someone else owns it), so a device build uses `com.orbit.app` followed by your team id. Firebase rejects that id (`API_KEY_IOS_APP_BLOCKED` in the console), so on a device the app runs but nothing that calls Firebase works until the app has a bundle id of its own.

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
