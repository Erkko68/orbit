package com.orbit.app.feature.account.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.navigation.OrbitNavHost
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.feature.account.AccountViewModel
import com.orbit.app.testing.FailingAuthRepository
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AccountScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private lateinit var failingAuth: FailingAuthRepository

    @BeforeTest
    fun setUp() {
        runTest {
            auth.register("ana@orbit.app", "secret", "Ana López")
            auth.updateProfile("Ana López", "en", TimeZone.UTC)
            failingAuth = FailingAuthRepository(user = auth.session.first())
        }
        startTestKoin(auth, InMemorySpaceRepository(auth))
    }

    @AfterTest
    fun stopDi() {
        stopKoin()
    }

    @Test
    fun displaysTheProfileAndDisablesSavingInvalidFields() = runComposeUiTest {
        setContent { OrbitTheme { AccountScreen(onBack = {}) } }
        onNodeWithText("Account").assertIsDisplayed()
        onNodeWithText("AL").assertIsDisplayed()
        onNodeWithContentDescription("Display name").assertTextContains("Ana López")
        onNodeWithText("English").performScrollTo().assertIsSelected()
        onNodeWithText("Save changes").performScrollTo().assertIsNotEnabled()

        onNodeWithContentDescription("Display name").performScrollTo().performTextReplacement(" ")
        onNodeWithText("Save changes").performScrollTo().assertIsNotEnabled()
        onNodeWithContentDescription("Display name").performScrollTo().performTextReplacement("Ana Maria")
        onNodeWithContentDescription("Time zone").performScrollTo().performClick()
        onNodeWithContentDescription("Search time zones").performTextReplacement("Rome")
        onNode(hasText("Europe/Rome") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .performScrollTo().performClick()
        onNodeWithText("Save changes").performScrollTo().assertIsEnabled()
    }

    @Test
    fun savesOnlyAfterConfirmationAndShowsTheUpdatedInitials() = runComposeUiTest {
        setContent { OrbitTheme { AccountScreen(onBack = {}) } }
        onNodeWithContentDescription("Display name").performScrollTo().performTextReplacement("Ana Maria")
        onNodeWithText("Español").performScrollTo().performClick().assertIsSelected()
        onNodeWithText("Català").performScrollTo().performClick().assertIsSelected()
        onNodeWithContentDescription("Time zone").performScrollTo().performClick()
        onNodeWithContentDescription("Search time zones").performTextReplacement("Madrid")
        onNode(hasText("Europe/Madrid") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .performScrollTo().performClick()
        runTest { assertEquals("Ana López", auth.session.first()?.displayName) }

        onNodeWithText("Save changes").performScrollTo().performClick()

        onNodeWithText("Profile updated.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Save changes").assertIsNotEnabled()
        onNodeWithText("AM").performScrollTo().assertIsDisplayed()
        runTest {
            val user = auth.session.first()!!
            assertEquals("Ana Maria", user.displayName)
            assertEquals("ca", user.language)
            assertEquals(TimeZone.of("Europe/Madrid"), user.timeZone)
        }
    }

    @Test
    fun showsWriteErrorsAndKeepsTheEditedName() = runComposeUiTest {
        val viewModel = AccountViewModel(failingAuth)
        setContent { OrbitTheme { AccountScreen(onBack = {}, viewModel = viewModel) } }
        onNodeWithContentDescription("Display name").performScrollTo().performTextReplacement("Ana Maria")
        onNodeWithText("Save changes").performScrollTo().performClick()

        onNodeWithText("Could not update your profile. Try again.").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Display name").performScrollTo().assertTextContains("Ana Maria")
        onNodeWithText("Save changes").performScrollTo().assertIsEnabled()
        onNodeWithText("Sign out").performScrollTo().performClick()
        onNodeWithText("Could not sign out. Try again.").performScrollTo().assertIsDisplayed()
        onNodeWithText("Sign out").performScrollTo().assertIsEnabled()
    }

    @Test
    fun opensFromTheSpacesAvatarReturnsAndSignsOutThroughTheAuthGate() = runComposeUiTest {
        setContent { OrbitTheme { OrbitNavHost() } }
        onNodeWithText("Orbit").assertIsDisplayed()
        onNodeWithContentDescription("Account").assertIsDisplayed().performClick()
        onNodeWithText("Account").assertIsDisplayed()

        onNodeWithContentDescription("Back to spaces").assertIsDisplayed().performClick()
        onNodeWithText("Orbit").assertIsDisplayed()
        onNodeWithContentDescription("Account").assertIsDisplayed().performClick()
        onNodeWithText("Sign out").performScrollTo().performClick()

        onNodeWithText("Welcome back").assertIsDisplayed()
        onNodeWithText("Sign in").assertIsDisplayed()
        onNodeWithText("Account").assertDoesNotExist()
        onNodeWithContentDescription("Back to spaces").assertDoesNotExist()
    }

    @Test
    fun cancellingTheTimeZonePickerLeavesTheProfileUnchanged() = runComposeUiTest {
        setContent { OrbitTheme { AccountScreen(onBack = {}) } }
        onNodeWithContentDescription("Time zone").performScrollTo().performClick()
        onNode(hasText(TimeZone.UTC.id) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .performScrollTo().assertIsSelected()
        onNodeWithContentDescription("Search time zones").performTextReplacement("no-such-zone")
        onNodeWithText("No time zones found.").assertIsDisplayed()
        onNodeWithText("Cancel").assertIsDisplayed().performClick()

        onNodeWithText("Choose a time zone").assertDoesNotExist()
        onNodeWithContentDescription("Time zone").assertTextContains(TimeZone.UTC.id)
        onNodeWithText("Save changes").performScrollTo().assertIsNotEnabled()
        runTest { assertEquals(TimeZone.UTC, auth.session.first()?.timeZone) }
    }

}
