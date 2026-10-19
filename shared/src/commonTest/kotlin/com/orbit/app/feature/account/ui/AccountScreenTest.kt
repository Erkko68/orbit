package com.orbit.app.feature.account.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement

import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.feature.account.AccountViewModel
import com.orbit.app.testing.FailingAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class AccountScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository().apply {
        runBlocking { register("ana@orbit.app", "secret", "Ana López") }
    }

    private fun ComposeUiTest.show(repository: AuthRepository = auth) =
        setContent { OrbitTheme { AccountScreen(onBack = {}, viewModel = AccountViewModel(repository)) } }

    /** The button of the open dialog that reads [text]. */
    private fun ComposeUiTest.dialogButton(text: String): SemanticsNodeInteraction =
        onNode(hasText(text) and hasClickAction() and hasAnyAncestor(isDialog()))

    @Test
    fun showsWhoIsSignedInAndTheirActions() = runComposeUiTest {
        show()

        onNodeWithText("Ana López").assertIsDisplayed()
        onNodeWithText("ana@orbit.app").assertIsDisplayed()
        onNodeWithText("Change password").performScrollTo().assertIsDisplayed()
        onNodeWithText("Sign out").performScrollTo().assertIsDisplayed()
        onNodeWithText("Delete account").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun renamesFromItsDialog() = runComposeUiTest {
        show()

        onNodeWithContentDescription("Edit name").performClick()
        onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextReplacement("Ana")
        dialogButton("Save").performClick()

        onNode(isDialog()).assertDoesNotExist()
        onNodeWithText("Ana").assertIsDisplayed()
        assertEquals("Ana", runBlocking { auth.session.first() }?.displayName)
    }

    @Test
    fun aWrongCurrentPasswordIsShownInTheDialog() = runComposeUiTest {
        show()

        onNodeWithText("Change password").performScrollTo().performClick()
        onNodeWithText("Current password").performTextReplacement("wrong")
        onNodeWithText("New password").performTextReplacement("new-secret")
        onNodeWithText("Confirm password").performTextReplacement("new-secret")
        dialogButton("Change password").performClick()

        onNodeWithText("That is not your current password.").assertIsDisplayed()
    }

    @Test
    fun deletesTheAccountOnceConfirmed() = runComposeUiTest {
        show()

        onNodeWithText("Delete account").performScrollTo().performClick()
        onNodeWithText("Delete your account?").assertIsDisplayed()
        dialogButton("Delete account").performClick()

        assertNull(runBlocking { auth.session.first() })
    }

    @Test
    fun aFailedSignOutSaysSo() = runComposeUiTest {
        show(FailingAuthRepository(user = User("ana", "Ana López", null, "en", TimeZone.UTC)))

        onNodeWithText("Sign out").performScrollTo().performClick()

        onNodeWithText("Could not sign out. Try again.").performScrollTo().assertIsDisplayed()
    }
}
