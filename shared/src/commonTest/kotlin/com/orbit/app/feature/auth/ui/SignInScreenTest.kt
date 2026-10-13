package com.orbit.app.feature.auth.ui

import androidx.compose.ui.semantics.SemanticsProperties
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
import com.orbit.app.feature.auth.SignInViewModel
import com.orbit.app.testing.FailingAuthRepository
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SignInScreenTest : ComposeTest() {
    @Test
    fun showsTheFormWithItsPlaceholdersDisabled() = runComposeUiTest {
        setContent {
            OrbitTheme {
                SignInScreen(SignInViewModel(InMemoryAuthRepository()))
            }
        }
        onNodeWithText("Welcome back").assertIsDisplayed()
        onNodeWithText("Email address").assertIsDisplayed()
        onNodeWithText("Password").assertIsDisplayed()
        onNodeWithText("Sign in").assertIsEnabled()

        onNodeWithText("Forgot password?").assertIsNotEnabled()
        onNodeWithText("Continue with Google").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Continue with Apple").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Create account").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun saysWhenSigningInFailed() = runComposeUiTest {
        setContent {
            OrbitTheme {
                SignInScreen(SignInViewModel(FailingAuthRepository()))
            }
        }
        onNodeWithText("Sign in").performScrollTo().performClick()
        onNodeWithText("Could not sign in. Check your details and try again.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun takesTheEmailAndShowsThePasswordOnRequest() = runComposeUiTest {
        setContent {
            OrbitTheme {
                SignInScreen(SignInViewModel(InMemoryAuthRepository()))
            }
        }
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
}
