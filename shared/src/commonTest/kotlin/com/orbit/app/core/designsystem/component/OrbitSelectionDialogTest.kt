package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitSelectionDialogTest : ComposeTest() {
    @Test
    fun displaysSelectionAndForwardsSearchSelectionAndDismissal() = runComposeUiTest {
        var query = ""
        var selected = ""
        var dismissals = 0
        setContent {
            OrbitTheme {
                OrbitSelectionDialog(
                    title = "Choose a time zone",
                    dismissText = "Cancel",
                    searchLabel = "Search time zones",
                    emptyText = "No time zones found.",
                    query = "Rome",
                    options = listOf(OrbitSelectionOption("Europe/Rome", selected = true)),
                    onQueryChange = { query = it },
                    onSelect = { selected = it },
                    onDismiss = { dismissals++ },
                )
            }
        }
        onNodeWithText("Europe/Rome").performScrollTo().assertIsSelected().performClick()
        assertEquals("Europe/Rome", selected)
        onNodeWithContentDescription("Search time zones").performTextReplacement("Madrid")
        assertEquals("Madrid", query)
        onNodeWithText("Cancel").assertIsDisplayed().performClick()
        assertEquals(1, dismissals)
    }

    @Test
    fun displaysAnEmptyResult() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitSelectionDialog(
                    title = "Choose a time zone",
                    dismissText = "Cancel",
                    searchLabel = "Search time zones",
                    emptyText = "No time zones found.",
                    query = "no-such-zone",
                    options = emptyList(),
                    onQueryChange = {},
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        onNodeWithText("No time zones found.").assertIsDisplayed()
    }
}
