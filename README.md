# Orbit

[![CI](https://github.com/Erkko68/orbit/actions/workflows/ci.yml/badge.svg)](https://github.com/Erkko68/orbit/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Erkko68_orbit&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Erkko68_orbit)
[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase)](https://firebase.google.com)

**Orbit** organises any group that shares responsibilities (flatmates, families, couples, trips, teams) on **Android** and **iOS**. Each group is a **space** whose members share tasks, rotating chores, bills, lists, events and expenses, synced in real time and usable offline.

> [!NOTE]
> **You stay in charge.** A cloud AI assistant asks a few short questions and proposes a plan for the space, but nothing it proposes is saved until you confirm it, and everything it does can also be done by hand.

## Features

- **Spaces and members**: email or Google sign-in, invitations by link or code, and roles (owner, admin, member).
- **One flexible item**: a title plus the modules it needs (a schedule, an assignee, a checklist, a reminder). Task, shopping list and event are presets.
- **Offline first**: everything reads from and writes to the local cache, and syncs when the connection is back.
- **Coming next**: the planning assistant, agenda and calendar, rotating chores, expenses and balances, a receipt scanner. See the [scope by sprint](docs/product/overview.md#scope-by-sprint).

## Platform support

- **Android**: minSdk 24, built and tested on every PR.
- **iOS**: built and tested on the simulator only. The project has no paid Apple Developer account, so it cannot sign for a device or get an APNs key: push notifications do not reach iOS. Local notifications work. A push can be simulated with `xcrun simctl push booted com.orbit.app <file>.apns`.

## Quick start

You need JDK 17 or later, Android Studio or IntelliJ IDEA, and Xcode for iOS. The Firebase app configs are in the repository, so the app runs against the project's Firebase as soon as it builds.

```sh
./gradlew :androidApp:assembleDebug        # Android, or the androidApp run configuration
open iosApp/iosApp.xcodeproj               # iOS: run from Xcode, it builds the shared framework through Gradle
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test   # tests
```

Firestore rules and their tests live in `firebase/`; see [Security rules](docs/data/security-rules.md#running-the-tests).

## Project structure

| Module | Contents |
| :--- | :--- |
| `shared` | All app code in Kotlin Multiplatform: Compose UI, ViewModels, domain, Firestore repositories, Koin. Platform code only where Android and iOS differ. |
| `androidApp` | The Android entry point. |
| `iosApp` | The Xcode project and SwiftUI entry point. |
| `firebase` | Firestore security rules and their emulator tests, Cloud Functions in Python. |
| `docs` | Architecture, schema, roles, workflow and decision records. |

Inside `shared`, code is split into `feature/` (screens and their ViewModels), `domain/` (pure Kotlin models and rules), `data/` (Firestore) and `core/` (design system, navigation, shared helpers). See the [architecture overview](docs/architecture/overview.md).

**Built with** Kotlin 2.4, Compose Multiplatform 1.12 with Material 3, Navigation Compose, Koin, Firebase (Authentication, Firestore, Cloud Functions) through GitLive, kotlinx coroutines, serialization and datetime, Multiplatform Settings, Coil and Kermit. Versions are in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Documentation

- **[Product overview](docs/product/overview.md)**: what Orbit does, its principles, the scope by sprint and a glossary.
- **[Architecture](docs/architecture/overview.md)**: modules, layers, navigation and one change end to end, then [State and ViewModels](docs/architecture/state-and-viewmodels.md), [Offline and sync](docs/architecture/offline-and-sync.md), [Authentication](docs/architecture/authentication.md) and the [Design system](docs/architecture/design-system.md).
- **[Firestore schema](docs/data/firestore-schema.md)**: every collection, field, query and write.
- **[Security rules and roles](docs/data/security-rules.md)**: who may read and change what.
- **[Decision records](docs/adr/README.md)**: why things are the way they are.
- **[Workflow](docs/process/workflow.md)** and **[Testing](docs/process/testing.md)**: from an issue to a merged pull request.
- **[Builds and deployment](docs/process/builds-and-deployment.md)**: what CI checks, how the apps are built and what is deployed to Firebase.

## Contributing

Work is tracked on the [project board](https://github.com/users/Erkko68/projects/3), with one milestone per sprint, and every change starts from an issue. Read [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow and [AGENTS.md](AGENTS.md) for the rules every change follows, whether written by a person or an AI agent.
