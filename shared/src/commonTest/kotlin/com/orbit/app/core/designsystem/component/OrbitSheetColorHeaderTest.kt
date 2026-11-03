package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitSheetColorHeaderTest : ComposeTest() {
    @Test
    fun showsWhatItIsAboutAndGoesBackOrCloses() = runComposeUiTest {
        val tapped = mutableListOf<String>()
        setContent {
            OrbitTheme {
                OrbitSheetColorHeader(
                    title = "Ben",
                    label = "Flat 4B",
                    closeDescription = "Close",
                    onClose = { tapped += "close" },
                    backDescription = "Back",
                    onBack = { tapped += "back" },
                    detail = "ADMIN",
                )
            }
        }

        onNodeWithText("Ben").assertIsDisplayed()
        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("ADMIN").assertIsDisplayed()
        onNodeWithContentDescription("Back").performClick()
        onNodeWithContentDescription("Close").performClick()

        assertEquals(listOf("back", "close"), tapped)
    }

    @Test
    fun withoutBackThereIsOnlyClose() = runComposeUiTest {
        setContent {
            OrbitTheme { OrbitSheetColorHeader(title = "Ben", label = "Flat 4B", closeDescription = "Close", onClose = {}) }
        }

        onNodeWithContentDescription("Back").assertDoesNotExist()
        onNodeWithContentDescription("Close").assertIsDisplayed()
    }
}
