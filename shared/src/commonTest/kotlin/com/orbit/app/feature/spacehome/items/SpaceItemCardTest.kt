package com.orbit.app.feature.spacehome.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.feature.item.icon
import com.orbit.app.feature.item.label
import com.orbit.app.domain.model.Preset
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.item_unknown_member
import orbit.shared.generated.resources.module_assignment_anyone
import orbit.shared.generated.resources.module_assignment_unassigned
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SpaceItemCardTest : ComposeTest() {
    @Test
    fun showsACompletedItemAndDisablesItsActionWhileSubmitting() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitScreen(scrollable = true) {
                    SpaceItemCard(
                        item = SpaceItem("done", "Completed task", Preset.TASK.label, Preset.TASK.icon, done = true),
                        onClick = {},
                        onDoneChange = {},
                        submitting = true,
                    )
                }
            }
        }

        onNodeWithText("Completed task").assertIsDisplayed()
        onNodeWithContentDescription("Reopen item").assertIsOn().assertIsNotEnabled()
        onNodeWithContentDescription("Complete item").assertDoesNotExist()
    }

    @Test
    fun showsThePresetProgressAndFooterAndOpensTheItem() = runComposeUiTest {
        var opens = 0
        val item = SpaceItem(
            id = "groceries",
            title = "Groceries",
            label = Preset.SHOPPING_LIST.label,
            icon = Preset.SHOPPING_LIST.icon,
            assignmentLabel = Res.string.item_unknown_member,
            assigneeName = "Ana López",
            dueDate = LocalDate(2026, 10, 26),
            checklist = SpaceItemChecklist(6, 9),
        )
        setContent {
            OrbitTheme {
                OrbitScreen(scrollable = true) {
                    SpaceItemCard(
                        item,
                        onClick = { opens++ },
                        onDoneChange = {},
                        submitting = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        onNodeWithText("List").assertIsDisplayed()
        val pill = onNodeWithText("List", useUnmergedTree = true).fetchSemanticsNode()
        val title = onNodeWithText("Groceries", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(pill.boundsInRoot.bottom <= title.boundsInRoot.top)
        onNodeWithText("6 of 9 done").performScrollTo().assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(6f / 9, 0f..1f))).assertIsDisplayed()
        onNodeWithText("AL").performScrollTo().assertIsDisplayed()
        onNodeWithText("Ana López").assertIsDisplayed()
        onNodeWithText("Oct 26, 2026").assertIsDisplayed()
        onNodeWithText("Groceries").performScrollTo().performClick()
        assertEquals(1, opens)
    }

    @Test
    fun showsTheDueDateWithoutAnAssignment() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitScreen(scrollable = true) {
                    SpaceItemCard(
                        SpaceItem(
                            "event",
                            "Dinner",
                            Preset.EVENT.label,
                            Preset.EVENT.icon,
                            dueDate = LocalDate(2026, 10, 26)
                        ),
                        onClick = {}, onDoneChange = {}, submitting = false,
                    )
                }
            }
        }

        onNodeWithText("Event").assertIsDisplayed()
        onNodeWithText("Oct 26, 2026").performScrollTo().assertIsDisplayed()
        onNodeWithText("Unknown member").assertDoesNotExist()
        onNodeWithText("Checklist").assertDoesNotExist()
    }

    @Test
    fun omitsMissingDetailsInDarkTheme() = runComposeUiTest {
        setContent {
            OrbitTheme(darkTheme = true) {
                OrbitScreen(scrollable = true) {
                    SpaceItemCard(
                        SpaceItem("plain", "Plain item", (null as Preset?).label, (null as Preset?).icon),
                        onClick = {},
                        onDoneChange = {},
                        submitting = false
                    )
                }
            }
        }

        onNodeWithText("Item").assertIsDisplayed()
        onNodeWithText("Plain item").assertIsDisplayed()
        onNodeWithText("Checklist").assertDoesNotExist()
        onNodeWithText("Unknown member").assertDoesNotExist()
        onNodeWithText("Anyone").assertDoesNotExist()
        onNodeWithText("Unassigned").assertDoesNotExist()
    }

    @Test
    fun showsAssignmentFallbacksAndAnEmptyChecklistInDarkTheme() = runComposeUiTest {
        setContent {
            OrbitTheme(darkTheme = true) {
                OrbitScreen(scrollable = true) {
                    SpaceItemCard(
                        SpaceItem(
                            "any",
                            "Anyone's task",
                            Preset.TASK.label, Preset.TASK.icon,
                            assignmentLabel = Res.string.module_assignment_anyone
                        ),
                        onClick = {}, onDoneChange = {}, submitting = false,
                    )
                    SpaceItemCard(
                        SpaceItem(
                            "claim",
                            "Unclaimed task",
                            Preset.TASK.label, Preset.TASK.icon,
                            assignmentLabel = Res.string.module_assignment_unassigned
                        ),
                        onClick = {}, onDoneChange = {}, submitting = false,
                    )
                    SpaceItemCard(
                        SpaceItem(
                            "unknown", "Empty list", Preset.SHOPPING_LIST.label, Preset.SHOPPING_LIST.icon,
                            assignmentLabel = Res.string.item_unknown_member,
                            checklist = SpaceItemChecklist(0, 0),
                        ),
                        onClick = {}, onDoneChange = {}, submitting = false,
                    )
                }
            }
        }

        onNodeWithText("Anyone").performScrollTo().assertIsDisplayed()
        onNodeWithText("Unassigned").performScrollTo().assertIsDisplayed()
        onNodeWithText("Unknown member").performScrollTo().assertIsDisplayed()
        onNodeWithText("0 of 0 done").performScrollTo().assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0f, 0f..1f))).assertIsDisplayed()
        onAllNodesWithText("AL").assertCountEquals(0)
    }
}
