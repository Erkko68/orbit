package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitPasswordFieldTest : ComposeTest() {
    @Test
    fun masksThePasswordAndAsksToShowIt() = runComposeUiTest {
        var visible by mutableStateOf(false)
        setContent {
            OrbitTheme {
                OrbitPasswordField("secret", {}, "Password", visible, onToggleVisibility = { visible = !visible })
            }
        }
        val shown = SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("secret"))
        onNodeWithText("secret").assert(!shown)

        onNodeWithContentDescription("Show password").performClick()

        onNodeWithText("secret").assert(shown)
        onNodeWithContentDescription("Hide password").assertIsDisplayed()
    }

    @Test
    fun showsItsError() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitPasswordField("", {}, "Password", false, {}, isError = true, supportingText = "Wrong password")
            }
        }
        onNodeWithText("Wrong password").assertIsDisplayed()
    }
}
