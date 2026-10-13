package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitErrorStateTest : ComposeTest() {
    @Test
    fun saysWhatFailedAndRetriesOnTap() = runComposeUiTest {
        var retries = 0
        setContent {
            OrbitTheme {
                OrbitErrorState(
                    title = "Could not load your spaces",
                    body = "Check your connection and try again.",
                    retryText = "Try again",
                    onRetry = { retries++ },
                )
            }
        }
        onNodeWithText("Could not load your spaces").assertIsDisplayed()
        onNodeWithText("Check your connection and try again.").assertIsDisplayed()

        onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var title by mutableStateOf("Could not load your spaces")
        var retries = 0
        var onRetry by mutableStateOf<() -> Unit>({})
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitErrorState(title, body = "Try later.", retryText = "Retry", onRetry = onRetry)
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        title = "Could not load items"
        onRetry = { retries++ }
        onNodeWithText("Could not load items").assertIsDisplayed()
        onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }
}
