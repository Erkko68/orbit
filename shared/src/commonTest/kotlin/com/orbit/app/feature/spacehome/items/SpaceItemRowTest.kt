package com.orbit.app.feature.spacehome.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.item.ItemFace
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SpaceItemRowTest : ComposeTest() {
    private val today = LocalDate(2026, 11, 2)

    @Test
    fun onlyAnOpenItemPastItsDayIsOverdue() {
        val yesterday = SpaceItem("1", "Bins", dueDate = LocalDate(2026, 11, 1))

        assertTrue(yesterday.isOverdue(today))
        assertFalse(yesterday.copy(done = true).isOverdue(today))
        assertFalse(yesterday.copy(dueDate = today).isOverdue(today))
        assertFalse(yesterday.copy(dueDate = null).isOverdue(today))
    }

    private fun androidx.compose.ui.test.ComposeUiTest.showRow(
        item: SpaceItem,
        darkTheme: Boolean = false,
        submitting: Boolean = false,
        onClick: () -> Unit = {},
        onDoneChange: (Boolean) -> Unit = {},
    ) = setContent {
        OrbitTheme(darkTheme = darkTheme) {
            OrbitScreen(scrollable = true) {
                SpaceItemRow(item, today, onClick, onDoneChange, submitting, Modifier.fillMaxWidth())
            }
        }
    }

    @Test
    fun aDoneItemOffersToReopenAndWaitsWhileSubmitting() = runComposeUiTest {
        showRow(SpaceItem("done", "Completed task", done = true), submitting = true)

        onNodeWithText("Completed task").assertIsDisplayed()
        onNodeWithContentDescription("Reopen Completed task").assertIsOn().assertIsNotEnabled()
        onNodeWithContentDescription("Complete Completed task").assertDoesNotExist()
    }

    @Test
    fun showsWhoAndWhenAndOpensTheItem() = runComposeUiTest {
        var opens = 0
        val item = SpaceItem(
            id = "plumber",
            title = "Book the plumber",
            summary = "Sink leaking",
            faces = listOf(ItemFace("ana", "Ana López", 1, null)),
            dueDate = today,
            dueTime = LocalTime(18, 0),
        )
        showRow(item, onClick = { opens++ })

        onNodeWithText("Sink leaking").assertIsDisplayed()
        onNodeWithText("TODAY · 18:00").assertIsDisplayed()
        onNodeWithContentDescription("Ana López", useUnmergedTree = true).assertExists()
        onNodeWithText("Book the plumber").performClick()
        assertEquals(1, opens)
    }

    @Test
    fun theCheckCompletesTheItem() = runComposeUiTest {
        val changes = mutableListOf<Boolean>()
        showRow(SpaceItem("task", "Water the plants"), onDoneChange = { changes += it })

        onNodeWithContentDescription("Complete Water the plants").performClick()

        assertEquals(listOf(true), changes)
    }

    @Test
    fun saysTheDaysAroundTodayInWords() = runComposeUiTest {
        showRow(SpaceItem("event", "Dinner", dueDate = LocalDate(2026, 11, 3)))

        onNodeWithText("TOMORROW").assertIsDisplayed()
    }

    @Test
    fun aDateFurtherAwayIsADate() = runComposeUiTest {
        showRow(SpaceItem("event", "Dinner", dueDate = LocalDate(2026, 12, 26)))

        onNodeWithText("DEC 26, 2026").assertIsDisplayed()
    }

    @Test
    fun aRowWithoutDetailsIsItsTitleInDarkTheme() = runComposeUiTest {
        showRow(SpaceItem("plain", "Plain item"), darkTheme = true)

        onNodeWithText("Plain item").assertIsDisplayed()
    }

    @Test
    fun aChecklistSaysHowFarItIs() = runComposeUiTest {
        showRow(SpaceItem("task", "Pack", checklist = SpaceItemChecklist(1, 3)))

        onNodeWithText("1 OF 3").assertIsDisplayed()
    }

    @Test
    fun theCheckOfAListOpensIt() = runComposeUiTest {
        var opens = 0
        val changes = mutableListOf<Boolean>()
        showRow(SpaceItem("list", "Pack", checklist = SpaceItemChecklist(1, 3)), onClick = { opens++ }, onDoneChange = { changes += it })

        onNodeWithContentDescription("Complete Pack").performClick()

        assertEquals(1, opens)
        assertTrue(changes.isEmpty())
    }
}
