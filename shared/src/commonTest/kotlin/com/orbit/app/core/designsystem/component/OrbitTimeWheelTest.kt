package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitTimeWheelTest : ComposeTest() {
    private var hours by mutableStateOf(1)
    private var minutes by mutableStateOf(30)

    private fun ComposeUiTest.showWheel() = setContent {
        OrbitTheme {
            OrbitTimeWheel(
                hours = hours,
                minutes = minutes,
                onHoursChange = { hours = it },
                onMinutesChange = { minutes = it },
                hoursLabel = "Hours",
                minutesLabel = "Min",
            )
        }
    }

    @Test
    fun showsWhatIsPickedOnEachWheelAndTheNumbersAroundIt() = runComposeUiTest {
        showWheel()

        onNodeWithContentDescription("Hours").assertTextEquals("01")
        onNodeWithContentDescription("Min").assertTextEquals("30")
        onNodeWithText("Hours").assertIsDisplayed()
    }

    @Test
    fun tappingANumberOpensTheKeyboardAndTheWheelStays() = runComposeUiTest {
        showWheel()

        onNodeWithContentDescription("Min").performClick()

        onNodeWithContentDescription("Min").assertIsFocused()
        assertEquals(30, minutes)
    }

    @Test
    fun whatIsTypedMovesTheWheel() = runComposeUiTest {
        showWheel()

        onNodeWithContentDescription("Min").performTextInput("4")
        waitForIdle()
        assertEquals(4, minutes)
        onNodeWithContentDescription("Min").assertTextEquals("04")

        onNodeWithContentDescription("Min").performTextInput("5")
        waitForIdle()
        assertEquals(45, minutes)
        onNodeWithContentDescription("Min").assertTextEquals("45")
        assertEquals(1, hours)
    }

    @Test
    fun onlyDigitsCountAndEmptyIsZero() = runComposeUiTest {
        showWheel()

        onNodeWithContentDescription("Hours").performTextReplacement("a4")
        waitForIdle()
        assertEquals(4, hours)

        onNodeWithContentDescription("Hours").performTextReplacement("")
        waitForIdle()
        assertEquals(0, hours)
        onNodeWithContentDescription("Hours").assertTextEquals("00")
    }

    @Test
    fun theKeyboardsDoneLeavesTheField() = runComposeUiTest {
        showWheel()
        onNodeWithContentDescription("Min").performClick()

        onNodeWithContentDescription("Min").performImeAction()

        assertEquals(30, minutes)
    }

    @Test
    fun followsItsInputs() = runComposeUiTest {
        showWheel()

        hours = 12
        waitForIdle()

        onNodeWithContentDescription("Hours").assertTextEquals("12")
    }
}
