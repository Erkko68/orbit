package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitAmountFieldTest : ComposeTest() {
    @Test
    fun typesAnAmountNamedForScreenReadersAndShowsThePlaceholderWhileEmpty() = runComposeUiTest {
        var typed = ""
        setContent { OrbitTheme { OrbitAmountField("", { typed = it }, "Share for Ana", "0,00") } }
        onNodeWithText("0,00").assertIsDisplayed()
        onNode(hasSetTextAction() and hasContentDescription("Share for Ana")).performTextInput("12,40")
        assertEquals("12,40", typed)
    }

    @Test
    fun aDisabledFieldTakesNoInput() = runComposeUiTest {
        setContent { OrbitTheme { OrbitAmountField("5", {}, "Share for Ana", "0,00", enabled = false, isError = true) } }
        onNodeWithContentDescription("Share for Ana").assertIsNotEnabled()
    }

    @Test
    fun showsTheCurrencyBesideTheAmount() = runComposeUiTest {
        setContent { OrbitTheme { OrbitAmountField("5", {}, "Share for Ana", "0,00", suffix = "€") } }
        onNodeWithText("€").assertIsDisplayed()
    }
}
