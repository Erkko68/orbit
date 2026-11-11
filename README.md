# Orbit

[![CI](https://github.com/Erkko68/orbit/actions/workflows/ci.yml/badge.svg)](https://github.com/Erkko68/orbit/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Erkko68_orbit&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Erkko68_orbit)
[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase)](https://firebase.google.com)

Orbit organises a group that shares responsibilities (flatmates, a family, a trip, a team) on Android and iOS. Each group is a **space**: its members share tasks, lists, events and expenses, synced in real time through Firebase and usable offline. One Kotlin Multiplatform codebase draws both apps with Compose.

## What it does

- **Spaces and members.** Create a space or join one with an 8-character code or a link. Owners and admins invite, rename and remove; the owner changes roles and hands the space over.
- **Items.** One form for every task, chore or list: who has it, when (with a time, a window or a repeat), a reminder, a description, a checklist and its history. Ticking it finishes it for everyone; a repeating one moves to its next date.
- **Tasks and Calendar.** "Your turn" shows what is yours next; overdue items stand out; the calendar shows the month and a list by day.
- **Expenses.** Who paid, split equally, by percentage, by weight or by amount; balances work out who owes whom and how to settle up in the fewest payments.
- **Assistant.** A short interview, typed or dictated, proposes a plan for the space. Nothing it proposes is saved until a member confirms it, and everything it does can also be done by hand.
- **Account.** Email or Google sign-in, either way into the same account, a forgotten password reset by email, and account deletion.

## Getting started

You need **JDK 17+**, **Android Studio** or **IntelliJ IDEA** with the Kotlin Multiplatform plugin, and **Xcode** for iOS. The Firebase configs are in the repository, so a build talks to the project's Firebase (`orbit-app-dev`) straight away.

| To | Run |
| :--- | :--- |
| Build and run on Android | The `androidApp` run configuration, or `./gradlew :androidApp:installDebug` |
| Run on iOS | `open iosApp/iosApp.xcodeproj`, pick a simulator, Run. Xcode builds the shared framework through Gradle |
| Run the app's tests | `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test` |
| Check what CI checks | `./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64` |
| Test the Firestore rules | From `firebase/`: `npm ci`, then `npx firebase emulators:exec --only firestore "npm test"` |
| Test the assistant server | From `server/`: `pip install -r requirements-dev.txt`, then `pytest` |

**The assistant** needs its own server ([ADR 0007](docs/adr/0007-assistant-on-our-own-server.md)). From `server/`, put `FIREBASE_API_KEY=…` in a `.env` file, run `docker compose up -d --build`, then once `docker compose exec ollama ollama pull gemma4:e2b`, and put HTTPS in front of port 8080. The app calls `ASSISTANT_SERVER_URL` in `ServerAssistantRepository.kt`, a placeholder until the server has an address. Without it, everything but the assistant works, and its manual path always does.

**Google sign-in on Android** needs your debug key's SHA-1 added to the Firebase Android app; without it, the sign-in fails with `NoCredentialException`.

## Where things are

| Path | What it holds |
| :--- | :--- |
| `shared/src/commonMain/.../feature/` | One folder per screen: its composables in `ui/`, its `ViewModel` and `UiState` |
| `shared/src/commonMain/.../domain/` | Plain Kotlin: models, validation, repository interfaces and use cases such as balances |
| `shared/src/commonMain/.../data/` | The Firebase repositories and Firestore mappers |
| `shared/src/commonMain/.../core/designsystem/` | The theme (`Color.kt`, `Type.kt`) and the `Orbit…` components every screen is built from |
| `shared/src/commonMain/composeResources/` | Strings in English, Spanish, Catalan and Italian, icons and fonts |
| `shared/src/commonTest/` | Unit and Compose UI tests, run on Android (Robolectric) and iOS; the fakes are in `testing/` |
| `androidApp/`, `iosApp/` | The platform entry points |
| `firebase/` | `firestore.rules` and their emulator tests |
| `server/` | The assistant: a small Python server in Docker, next to its model |
| `docs/` | Architecture, Firestore schema, roles, workflow and decision records |

A change usually goes UI → `ViewModel` → repository interface in `domain/` → Firebase implementation in `data/`. The [architecture overview](docs/architecture/overview.md) walks through one end to end.

## Platform notes

- **Android**: minSdk 24, built and tested on every pull request.
- **iOS**: runs on the simulator. Without a paid Apple Developer account the app cannot be signed for a device or receive push notifications; local reminders work, and a push can be simulated with `xcrun simctl push booted com.orbit.app <file>.apns`.

## Documentation

- [Product overview](docs/product/overview.md): what Orbit does, its principles, the scope by sprint and a glossary.
- [Architecture](docs/architecture/overview.md), then [State and ViewModels](docs/architecture/state-and-viewmodels.md), [Offline and sync](docs/architecture/offline-and-sync.md), [Authentication](docs/architecture/authentication.md) and the [Design system](docs/architecture/design-system.md).
- [Firestore schema](docs/data/firestore-schema.md) and [Security rules and roles](docs/data/security-rules.md).
- [Decision records](docs/adr/README.md): why things are the way they are.
- [Workflow](docs/process/workflow.md), [Testing](docs/process/testing.md) and [Builds and deployment](docs/process/builds-and-deployment.md).

## Contributing

Every change starts from an issue on the [project board](https://github.com/users/Erkko68/projects/3) and lands through a reviewed pull request. [CONTRIBUTING.md](CONTRIBUTING.md) has the workflow and [AGENTS.md](AGENTS.md) the rules every change follows, whether a person or an AI agent writes it.
