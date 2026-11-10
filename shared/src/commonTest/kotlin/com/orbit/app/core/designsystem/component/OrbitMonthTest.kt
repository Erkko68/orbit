package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitMonthTest : ComposeTest() {
    @Test
    fun showsTheMonthMarksDaysAndPicksOne() = runComposeUiTest {
        var selected by mutableStateOf(LocalDate(2026, 11, 10))
        var moves = 0
        setContent {
            OrbitTheme {
                OrbitMonth(
                    month = LocalDate(2026, 11, 1),
                    today = LocalDate(2026, 11, 10),
                    selected = selected,
                    marked = setOf(LocalDate(2026, 11, 20)),
                    markedLabel = "Has items",
                    onSelect = { selected = it },
                    previousLabel = "Previous month",
                    onPrevious = { moves-- },
                    nextLabel = "Next month",
                    onNext = { moves++ },
                )
            }
        }

        onNodeWithText("November 2026").assertIsDisplayed()
        onNodeWithText("30").assertIsDisplayed()
        onNodeWithText("31").assertDoesNotExist()
        onNodeWithText("10").assertIsSelected()
        onNode(hasText("20") and SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Has items"))
            .assertIsDisplayed()

        onNodeWithText("20").performClick()
        assertEquals(LocalDate(2026, 11, 20), selected)
        onNodeWithText("20").assertIsSelected()

        onNodeWithContentDescription("Next month").performClick()
        onNodeWithContentDescription("Previous month").performClick()
        onNodeWithContentDescription("Previous month").performClick()
        assertEquals(-1, moves)
    }
}
