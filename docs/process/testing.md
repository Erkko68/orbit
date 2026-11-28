# Testing

## What is tested where

| Kind | Source set | Runs on | Command |
|---|---|---|---|
| Unit tests: domain, mappers, ViewModels | `shared/src/commonTest` | Android host (JVM) and iOS simulator | `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test` |
| UI tests: screens and design-system components | `shared/src/commonTest` | Android host through Robolectric, and iOS | Same |
| End-to-end flows: the whole app on fakes | `shared/src/commonTest/…/e2e` | Android host through Robolectric, and iOS | Same |
| Android-only code: notifications, Google sign-in, navigation | `shared/src/androidHostTest` | Android host | `./gradlew :shared:testAndroidHostTest` |
| iOS-only code | `shared/src/iosTest` | iOS simulator | `./gradlew :shared:iosSimulatorArm64Test` |
| Firestore rules | `firebase/tests/*.test.js` | Firestore emulator | See [below](#firestore-rules) |

The Firebase repositories call the live SDK and cannot run in host tests. ViewModels and screens are tested against fakes instead.

## What must have a test

- **Every ViewModel**, including the failure path of each read and each write. A read fails into `loadFailed` (or a fallback), a write into its failure flag.
- **Every screen and design-system component**, with a UI test.
- **Every mapper and domain rule.**
- **Every rule** in `firestore.rules`, allowed and denied.
- **A bug fix**, with a test that fails without it.

## Fakes

Shared test doubles live in `shared/src/commonTest/kotlin/com/orbit/app/testing/`, never next to the first test that needed them. See the table in [State and ViewModels](../architecture/state-and-viewmodels.md#testing-a-viewmodel).

## Writing a ViewModel test

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class SpaceMembersViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun saysWhenTheMembersFailToLoadAndLoadsThemOnRetry() = runTest {
        val viewModel = SpaceMembersViewModel("space-1", FlakySpaceRepository(memberFailures = 1))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.loadFailed)

        viewModel.retry()

        assertEquals(listOf("ana"), viewModel.uiState.value.members.map { it.name })
    }
}
```

`uiState` only runs while collected (`WhileSubscribed`), hence the collector in `backgroundScope`.

## Writing a UI test

The class extends `ComposeTest` and uses `runComposeUiTest` from `androidx.compose.ui.test.v2`, wrapped in `OrbitTheme`. A screen whose components resolve their own ViewModels starts Koin with `startTestKoin(…)` and stops it after. The host test window is small: `performScrollTo()` before `performClick()`, or the click lands off screen and does nothing.

## End-to-end flows

`shared/src/commonTest/kotlin/com/orbit/app/e2e/MainFlowsTest.kt` runs the whole app, `App()` with its real navigation, on the fakes from `startTestKoin`. It checks that the screens join up into what a user does:

- sign in and create a space;
- add an item and complete it;
- add an expense and settle up;
- review and confirm an assistant plan, with nothing saved before the confirmation.

These run in CI with the other UI tests, on Android and iOS. A new main flow gets a test here; the details of a screen stay in that screen's own test.

## Two-device checklist

Fakes cannot show sync between phones, offline work or notifications. Run this by hand on two Android phones (or a phone and an emulator) against Firebase before each sprint review. Use two accounts, A and B, and a fresh space. Note the date, the build and anything that failed in the sprint's review issue.

**Joining and sync**

- [ ] A creates a space and shares an invitation. B joins with the code. Each phone shows the other in the space's faces within a few seconds.
- [ ] A adds an item with a date. It appears on B's Tasks and Calendar without a refresh.
- [ ] B renames it and ticks a checklist entry. A sees both changes.
- [ ] A and B edit different fields of the same item at the same time. Both edits are kept.
- [ ] B completes the item. A sees it done, by B. A turn item moves to its next person on both phones.
- [ ] A adds an expense paid by A and split evenly. B's balance shows that B owes A. B marks it as paid, and both phones show settled up.

**Offline**

- [ ] B turns on airplane mode and adds, edits and completes an item. Each change shows at once on B.
- [ ] The assistant is disabled on B and says it needs a connection. Adding by hand still works.
- [ ] B turns airplane mode off. A gets B's offline changes, and nothing is duplicated or lost.
- [ ] B closes the app while offline, reopens it and still sees the space and its items from the cache.

**Roles**

- [ ] As a member, B cannot delete A's item, manage members or invite. As owner, A can.
- [ ] A removes B. The space disappears from B's list.

**Notifications (Android)**

- [ ] B allows notifications. An item with a reminder for B fires on B's phone at its time, also with the app closed.
- [ ] A moves the item's date. B's reminder moves too; completing the item cancels it.
- [ ] Push notices of other members' changes (#102, #90), once they land.

## Firestore rules

From `firebase/`, with Node and a Java runtime installed:

```sh
npm ci
npx firebase emulators:exec --only firestore "npm test"
```

Each `describe` block seeds the documents it needs with the rules disabled, then checks one rule as different users with `assertSucceeds` and `assertFails`.

## Coverage

Kover measures the Android host tests (`./gradlew :shared:koverXmlReport`), and SonarCloud requires 80% coverage on new code. The assistant server reports its own coverage with pytest-cov (see [Assistant server](../backend/assistant-server.md#tests-and-coverage)). `.github/scripts/**` is excluded until it has its own report.
