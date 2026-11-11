package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitMoneyFieldTest : ComposeTest() {
    @Test
    fun showsTheLabelThePlaceholderAndTheCurrency() = runComposeUiTest {
        setContent { OrbitTheme { OrbitMoneyField("", {}, label = "Amount", placeholder = "0.00", suffix = "€") } }

        onNodeWithText("Amount").assertIsDisplayed()
        onNodeWithText("0.00").assertIsDisplayed()
        onNodeWithText("€").assertIsDisplayed()
    }

    @Test
    fun passesOnWhatIsTyped() = runComposeUiTest {
        var typed = ""
        setContent {
            var value by remember { mutableStateOf("") }
            OrbitTheme {
                OrbitMoneyField(value, { value = it; typed = it }, label = "Amount", placeholder = "0.00")
            }
        }

        onNodeWithContentDescription("Amount").performTextInput("12.40")

        assertEquals("12.40", typed)
    }

    @Test
    fun saysWhyAnAmountIsWrong() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitMoneyField("0", {}, label = "Amount", placeholder = "0.00", isError = true, errorText = "Enter an amount")
            }
        }

        onNodeWithText("Enter an amount").assertIsDisplayed()
    }
}
