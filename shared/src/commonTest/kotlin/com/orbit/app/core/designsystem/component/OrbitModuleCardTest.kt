package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_list
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitModuleCardTest : ComposeTest() {
    @Test
    fun showsItsTitleAndContent() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitModuleCard("Schedule", Res.drawable.ic_event, "Remove schedule", onRemove = {}) {
                    Text("Fri 9 Oct, 20:00")
                }
            }
        }
        onNodeWithText("Schedule").assertIsDisplayed()
        onNodeWithText("Fri 9 Oct, 20:00").assertIsDisplayed()
    }

    @Test
    fun removesFromALabelledButtonWithA48dpTarget() = runComposeUiTest {
        var removed = 0
        setContent {
            OrbitTheme {
                OrbitModuleCard("Checklist", Res.drawable.ic_list, "Remove checklist", onRemove = { removed++ }) {}
            }
        }
        onNodeWithContentDescription("Remove checklist")
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performClick()
        assertEquals(1, removed)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var removed = 0
        var title by mutableStateOf("Schedule")
        var removeDescription by mutableStateOf("Remove schedule")
        var onRemove by mutableStateOf<() -> Unit>({})
        var detail by mutableStateOf("Fri 9 Oct, 20:00")
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitModuleCard(
                    title = title,
                    icon = Res.drawable.ic_event,
                    removeDescription = removeDescription,
                    onRemove = onRemove,
                    modifier = Modifier.testTag("module"),
                ) {
                    Text(detail)
                }
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()
        onNodeWithTag("module").assertIsDisplayed()

        title = "Reminder"
        removeDescription = "Remove reminder"
        detail = "10 minutes before"
        onRemove = { removed++ }
        onNodeWithText("10 minutes before").assertIsDisplayed()
        onNodeWithContentDescription("Remove reminder").performClick()
        assertEquals(1, removed)
    }
}
