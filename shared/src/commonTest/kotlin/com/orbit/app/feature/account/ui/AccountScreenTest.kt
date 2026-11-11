package com.orbit.app.feature.account.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.feature.account.AccountViewModel
import com.orbit.app.testing.FailingAuthRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class AccountScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository().apply {
        runBlocking { register("ana@orbit.app", "secret", "Ana López") }
    }

    private fun ComposeUiTest.show(repository: AuthRepository = auth) =
        setContent { OrbitTheme { AccountScreen(onBack = {}, viewModel = AccountViewModel(repository, InMemorySpaceRepository(repository))) } }

    /** The button of the open sheet that reads [text]: the last one, as a row of the screen may read the same. */
    private fun ComposeUiTest.dialogButton(text: String): SemanticsNodeInteraction =
        onAllNodes(hasText(text) and hasClickAction()).onLast()

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

        onNodeWithText("Edit name").performScrollTo().performClick()
        onNode(hasSetTextAction()).performTextReplacement("Ana")
        dialogButton("Save").performClick()

        onNode(hasSetTextAction()).assertDoesNotExist()
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
        onNodeWithText("Your password").performTextReplacement("secret")
        dialogButton("Delete account").performClick()
        // Deleting reads the spaces before it deletes anything, so it is not over when the click returns.
        waitForIdle()

        assertNull(runBlocking { auth.session.first() })
    }

    @Test
    fun signsOutOnceConfirmed() = runComposeUiTest {
        show()

        onNodeWithText("Sign out").performScrollTo().performClick()
        onNodeWithText("Sign out?").assertIsDisplayed()
        dialogButton("Sign out").performClick()

        assertNull(runBlocking { auth.session.first() })
    }

    @Test
    fun cancellingKeepsTheUserSignedIn() = runComposeUiTest {
        show()

        onNodeWithText("Sign out").performScrollTo().performClick()
        onNodeWithContentDescription("Close").performClick()

        onNodeWithText("Sign out?").assertDoesNotExist()
        assertEquals("Ana López", runBlocking { auth.session.first() }?.displayName)
    }

    @Test
    fun aFailedSignOutSaysSoInTheDialog() = runComposeUiTest {
        show(FailingAuthRepository(user = User("ana", "Ana López", null)))

        onNodeWithText("Sign out").performScrollTo().performClick()
        dialogButton("Sign out").performClick()

        onNodeWithText("Something went wrong. Try again.").assertIsDisplayed()
    }
    @Test
    fun anAccountThatOnlySignsInWithGoogleIsNotOfferedAPasswordChange() = runComposeUiTest {
        val google = InMemoryAuthRepository().apply { runBlocking { signInWithGoogle() } }
        show(google)

        onNodeWithText("Edit name").performScrollTo().assertIsDisplayed()
        onNodeWithText("Delete account").performScrollTo().assertIsDisplayed()
        onNodeWithText("Change password").assertDoesNotExist()
    }

    @Test
    fun saysWhichSpacesToHandOverBeforeTheAccountCanGo() = runComposeUiTest {
        val spaces = InMemorySpaceRepository(auth)
        runBlocking {
            val owned = spaces.createSpace("Flat 4B", accent = 0)
            spaces.addMember(owned, Member("bob@orbit.app", "Bob", photoUrl = null, role = Role.MEMBER))
        }
        setContent { OrbitTheme { AccountScreen(onBack = {}, viewModel = AccountViewModel(auth, spaces)) } }

        onNodeWithText("Delete account").performScrollTo().performClick()
        onNodeWithText("Your password").performTextReplacement("secret")
        dialogButton("Delete account").performClick()
        waitForIdle()

        onNodeWithText("Hand these spaces over to someone else first, or delete them: Flat 4B").assertIsDisplayed()
        assertEquals("ana@orbit.app", runBlocking { auth.session.first() }?.id)
    }

    @Test
    fun aWrongPasswordKeepsTheAccountAndSaysSo() = runComposeUiTest {
        show()

        onNodeWithText("Delete account").performScrollTo().performClick()
        onNodeWithText("Your password").performTextReplacement("not-it")
        dialogButton("Delete account").performClick()

        onNodeWithText("That is not your current password.").assertIsDisplayed()
        assertEquals("ana@orbit.app", runBlocking { auth.session.first() }?.id)
    }

}
