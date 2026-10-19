# Contributing to Orbit

All work starts from an issue on the [project board](https://github.com/users/Erkko68/projects/3), with one milestone per sprint. No issue, no commit: if the work has none, create it first.

## One-time setup

- JDK 17 or later, Android Studio or IntelliJ IDEA, and Xcode for iOS.
- The GitHub CLI, logged in:
  ```sh
  brew install gh        # or https://cli.github.com
  gh auth login
  ```
- For Firestore rules work: Node, then `npm ci` in `firebase/`.

## The short version

1. Take a free issue of the current milestone whose blockers are closed, and assign yourself the day you start (`gh issue edit <n> --add-assignee @me`). The board moves the card for you.
2. Branch as `<type>/<issue>-short-name` from an up-to-date `main`.
3. Commit small, in Conventional Commits ending with `(#<issue>)`.
4. Build and test before pushing:
   ```sh
   ./gradlew :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64
   ./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test
   ```
5. Open one PR per complete feature, with `Closes #<issue>`. It needs one approval, resolved conversations and green checks, and is squash-merged.

The full walk-through is [docs/process/workflow.md](docs/process/workflow.md). The rules every change follows are in [AGENTS.md](AGENTS.md), and the rest of the documentation starts at [docs/](docs/README.md).
