package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitTextFieldTest : ComposeTest() {
    @Test
    fun keepsTheLabelVisibleWhenTheFieldHasAValue() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitTextField("Ana", onValueChange = {}, placeholder = "Name", label = "Display name")
            }
        }
        onNodeWithText("Display name").assertIsDisplayed()
        onNodeWithText("Ana").assertIsDisplayed()
    }

    @Test
    fun showsThePlaceholderUntilSomethingIsTyped() = runComposeUiTest {
        var value by mutableStateOf("")
        setContent {
            OrbitTheme {
                OrbitTextField(value, onValueChange = { value = it }, placeholder = "Email address")
            }
        }
        onNodeWithText("Email address").assertIsDisplayed()

        onNode(hasSetTextAction()).performTextInput("ana@orbit.app")

        assertEquals("ana@orbit.app", value)
        onNodeWithText("Email address").assertDoesNotExist()
    }

    @Test
    fun showsItsSupportingTextAndReportsErrors() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Password",
                    isError = true,
                    supportingText = "Use at least 8 characters.",
                )
            }
        }
        onNodeWithText("Use at least 8 characters.").assertIsDisplayed()
        onNode(hasSetTextAction()).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test
    fun staysEditableOnACard() = runComposeUiTest {
        var value by mutableStateOf("")
        setContent {
            OrbitTheme {
                OrbitCard {
                    OrbitTextField(value, onValueChange = { value = it }, placeholder = "Invite code or link")
                }
            }
        }
        onNode(hasSetTextAction()).performTextInput("AB12CD")
        assertEquals("AB12CD", value)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var placeholder by mutableStateOf("Password")
        var supportingText by mutableStateOf<String?>(null)
        var isError by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = placeholder,
                    modifier = Modifier.testTag("field"),
                    enabled = enabled,
                    isError = isError,
                    supportingText = supportingText,
                    leadingIcon = { Text("lock") },
                    trailingIcon = { Text("show") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        placeholder = "New password"
        supportingText = "Too short"
        isError = true
        enabled = false
        onNodeWithText("Too short").assertIsDisplayed()
        onNodeWithTag("field").assertIsNotEnabled()
    }
}
