# Testing

## What is tested where

| Kind | Source set | Runs on | Command |
|---|---|---|---|
| Unit tests: domain, mappers, ViewModels | `shared/src/commonTest` | Android host (JVM) and iOS simulator | `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test` |
| UI tests: screens and design-system components | `shared/src/commonTest` | Android host through Robolectric, and iOS | Same |
| Android-only code: notifications, Google sign-in, navigation | `shared/src/androidHostTest` | Android host | `./gradlew :shared:testAndroidHostTest` |
| iOS-only code | `shared/src/iosTest` | iOS simulator | `./gradlew :shared:iosSimulatorArm64Test` |
| Firestore rules | `firebase/tests/rules.test.js` | Firestore emulator | See [below](#firestore-rules) |

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

## Firestore rules

From `firebase/`, with Node and a Java runtime installed:

```sh
npm ci
npx firebase emulators:exec --only firestore "npm test"
```

Each `describe` block seeds the documents it needs with the rules disabled, then checks one rule as different users with `assertSucceeds` and `assertFails`.

## Coverage

Kover measures the Android host tests (`./gradlew :shared:koverXmlReport`), and SonarCloud requires 80% coverage on new code. `firebase/**` and `.github/scripts/**` are excluded until they have their own reports.
