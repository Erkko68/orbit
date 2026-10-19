package com.orbit.app.feature.auth.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.feature.auth.CreateAccountViewModel
import com.orbit.app.testing.FailingAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CreateAccountScreenTest : ComposeTest() {
    private fun ComposeUiTest.showCreateAccount(
        auth: AuthRepository = InMemoryAuthRepository(),
        onSignIn: () -> Unit = {},
    ) = setContent {
        OrbitTheme {
            CreateAccountScreen(onSignIn, CreateAccountViewModel(auth))
        }
    }

    private fun ComposeUiTest.fill(email: String, password: String, confirmation: String) {
        onNodeWithText("Email address").performTextInput(email)
        onNodeWithText("Password").performTextInput(password)
        onNodeWithText("Confirm password").performTextInput(confirmation)
    }

    @Test
    fun showsTheFormOfTheMockup() = runComposeUiTest {
        showCreateAccount()
        onNodeWithText("Create your account").assertIsDisplayed()
        onNodeWithText("Email address").assertIsDisplayed()
        onNodeWithText("Password").assertIsDisplayed()
        onNodeWithText("Confirm password").assertIsDisplayed()
        onNodeWithText("Use at least 8 characters.").assertIsDisplayed()
        onNodeWithText("Create account").assertIsEnabled()
        onNodeWithText("Continue with Google").performScrollTo().assertIsEnabled()
        onNodeWithText("Continue with Apple").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Already have an account?").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun goesBackToSignIn() = runComposeUiTest {
        var signIns = 0
        showCreateAccount(onSignIn = { signIns++ })

        onNodeWithText("Sign in").performScrollTo().performClick()

        assertEquals(1, signIns)
    }

    @Test
    fun showsAndHidesEachPasswordOnItsOwn() = runComposeUiTest {
        showCreateAccount()
        onAllNodesWithContentDescription("Show password").assertCountEquals(2)

        onAllNodesWithContentDescription("Show password")[0].performClick()
        onAllNodesWithContentDescription("Hide password").assertCountEquals(1)
        onAllNodesWithContentDescription("Show password").assertCountEquals(1)

        onAllNodesWithContentDescription("Show password")[0].performClick()
        onAllNodesWithContentDescription("Hide password").assertCountEquals(2)

        onAllNodesWithContentDescription("Hide password")[1].performClick()
        onAllNodesWithContentDescription("Hide password").assertCountEquals(1)
    }

    @Test
    fun saysWhatIsMissingWhenTheFormIsEmpty() = runComposeUiTest {
        showCreateAccount()

        onNodeWithText("Create account").performScrollTo().performClick()

        onNodeWithText("Enter your email address.").assertIsDisplayed()
    }

    @Test
    fun saysWhenThePasswordsDoNotMatch() = runComposeUiTest {
        showCreateAccount()
        fill("ana@orbit.app", "12345678", "12345679")

        onNodeWithText("Create account").performScrollTo().performClick()

        onNodeWithText("The passwords do not match.").assertIsDisplayed()
    }

    @Test
    fun createsTheAccountAndSignsIn() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        showCreateAccount(auth)
        fill("ana@orbit.app", "12345678", "12345678")

        onNodeWithText("Create account").performScrollTo().performClick()

        assertEquals("ana@orbit.app", runBlocking { auth.session.first() }?.id)
    }

    @Test
    fun saysWhenTheEmailAlreadyHasAnAccount() = runComposeUiTest {
        showCreateAccount(FailingAuthRepository(AuthException(AuthError.EMAIL_IN_USE)))
        fill("ana@orbit.app", "12345678", "12345678")

        onNodeWithText("Create account").performScrollTo().performClick()

        onNodeWithText("An account with this email already exists.").assertIsDisplayed()
    }

    @Test
    fun saysWhenThePasswordIsTooWeakForFirebase() = runComposeUiTest {
        showCreateAccount(FailingAuthRepository(AuthException(AuthError.WEAK_PASSWORD)))
        fill("ana@orbit.app", "12345678", "12345678")

        onNodeWithText("Create account").performScrollTo().performClick()

        onNodeWithText("Choose a stronger password.").assertIsDisplayed()
    }

    @Test
    fun signsInWithGoogle() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        showCreateAccount(auth)

        onNodeWithText("Continue with Google").performScrollTo().performClick()

        assertEquals("google@orbit.app", runBlocking { auth.session.first() }?.id)
    }

    @Test
    fun saysWhyGoogleSignInFailed() = runComposeUiTest {
        showCreateAccount(FailingAuthRepository(AuthException(AuthError.NETWORK)))

        onNodeWithText("Continue with Google").performScrollTo().performClick()

        onNodeWithText("No connection. Check your internet and try again.").assertIsDisplayed()
    }
}
