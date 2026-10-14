package com.orbit.app.feature.auth.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.feature.auth.SignInViewModel
import com.orbit.app.testing.FailingAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SignInScreenTest : ComposeTest() {
    private fun ComposeUiTest.showSignIn(
        auth: AuthRepository = InMemoryAuthRepository(),
        onCreateAccount: () -> Unit = {},
    ) = setContent {
        OrbitTheme {
            SignInScreen(onCreateAccount, SignInViewModel(auth))
        }
    }

    @Test
    fun showsTheFormWithItsPlaceholdersDisabled() = runComposeUiTest {
        showSignIn()
        onNodeWithText("Welcome back").assertIsDisplayed()
        onNodeWithText("Email address").assertIsDisplayed()
        onNodeWithText("Password").assertIsDisplayed()
        onNodeWithText("Sign in").assertIsEnabled()
        onNodeWithText("Create account").performScrollTo().assertIsEnabled()

        onNodeWithText("Forgot password?").assertIsNotEnabled()
        onNodeWithText("Continue with Google").performScrollTo().assertIsEnabled()
        onNodeWithText("Continue with Apple").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun opensCreateAccount() = runComposeUiTest {
        var opened = 0
        showSignIn(onCreateAccount = { opened++ })

        onNodeWithText("Create account").performScrollTo().performClick()

        assertEquals(1, opened)
    }

    @Test
    fun saysWhatIsMissingWhenTheFormIsEmpty() = runComposeUiTest {
        showSignIn()

        onNodeWithText("Sign in").performScrollTo().performClick()

        onNodeWithText("Enter your email address.").assertIsDisplayed()
        onNodeWithText("Enter your password.").assertIsDisplayed()
    }

    @Test
    fun saysWhenTheEmailIsNotAnEmail() = runComposeUiTest {
        showSignIn()
        onNodeWithText("Email address").performTextInput("ana")
        onNodeWithText("Password").performTextInput("secret")

        onNodeWithText("Sign in").performScrollTo().performClick()

        onNodeWithText("Enter a valid email address.").assertIsDisplayed()
    }

    @Test
    fun saysWhyItCouldNotSignIn() = runComposeUiTest {
        showSignIn(FailingAuthRepository(AuthException(AuthError.WRONG_CREDENTIALS)))
        onNodeWithText("Email address").performTextInput("ana@orbit.app")
        onNodeWithText("Password").performTextInput("wrong")

        onNodeWithText("Sign in").performScrollTo().performClick()

        onNodeWithText("Wrong email or password.").assertIsDisplayed()
    }

    @Test
    fun saysWhenThereIsNoConnection() = runComposeUiTest {
        showSignIn(FailingAuthRepository(AuthException(AuthError.NETWORK)))
        onNodeWithText("Email address").performTextInput("ana@orbit.app")
        onNodeWithText("Password").performTextInput("secret")

        onNodeWithText("Sign in").performScrollTo().performClick()

        onNodeWithText("No connection. Check your internet and try again.").assertIsDisplayed()
    }

    @Test
    fun showsAnEmailProblemFromFirebaseUnderTheEmail() = runComposeUiTest {
        showSignIn(FailingAuthRepository(AuthException(AuthError.INVALID_EMAIL)))
        onNodeWithText("Email address").performTextInput("ana@orbit.app")
        onNodeWithText("Password").performTextInput("secret")

        onNodeWithText("Sign in").performScrollTo().performClick()

        onNodeWithText("Enter a valid email address.").assertIsDisplayed()
    }

    @Test
    fun takesTheEmailAndShowsThePasswordOnRequest() = runComposeUiTest {
        showSignIn()
        onNodeWithText("Email address").performTextInput("ana@orbit.app")
        onNodeWithText("ana@orbit.app").assertIsDisplayed()

        // The field keeps what was typed but displays it masked until Show password.
        val shown = SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("secret"))
        onNodeWithText("Password").performTextInput("secret")
        onNodeWithText("secret").assert(!shown)

        onNodeWithContentDescription("Show password").performClick()
        onNodeWithText("secret").assert(shown)
        onNodeWithContentDescription("Hide password").assertIsDisplayed()
    }

    @Test
    fun signsInWithGoogle() = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        showSignIn(auth)

        onNodeWithText("Continue with Google").performScrollTo().performClick()

        assertEquals("google@orbit.app", runBlocking { auth.session.first() }?.id)
    }

    @Test
    fun saysWhyGoogleSignInFailed() = runComposeUiTest {
        showSignIn(FailingAuthRepository(AuthException(AuthError.NETWORK)))

        onNodeWithText("Continue with Google").performScrollTo().performClick()

        onNodeWithText("No connection. Check your internet and try again.").assertIsDisplayed()
    }
}
