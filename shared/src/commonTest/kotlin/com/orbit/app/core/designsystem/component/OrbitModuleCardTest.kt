package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitModuleCardTest : ComposeTest() {
    @Test
    fun showsItsIconTitleAndContent() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitModuleCard("Schedule", icon = { Box(Modifier.testTag("icon")) }, onRemove = {}) {
                    Text("Fri 9 Oct, 20:00")
                }
            }
        }
        onNodeWithTag("icon", useUnmergedTree = true).assertExists()
        onNodeWithText("Schedule").assertIsDisplayed()
        onNodeWithText("Fri 9 Oct, 20:00").assertIsDisplayed()
    }

    @Test
    fun removesFromALabelledButtonWithA48dpTarget() = runComposeUiTest {
        var removed = 0
        setContent {
            OrbitTheme {
                OrbitModuleCard("Checklist", icon = {}, onRemove = { removed++ }) {}
            }
        }
        // The icon button draws at 40dp; M3 extends its touch area to 48dp.
        onNodeWithContentDescription("Remove Checklist")
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performClick()
        assertEquals(1, removed)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var removed = 0
        var title by mutableStateOf("Schedule")
        var detail by mutableStateOf("Fri 9 Oct, 20:00")
        var onRemove by mutableStateOf<() -> Unit>({})
        var icon by mutableStateOf<@Composable () -> Unit>({ Box(Modifier) })
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitModuleCard(
                    title = title,
                    icon = icon,
                    onRemove = onRemove,
                    modifier = Modifier.testTag("module"),
                ) {
                    Text(detail)
                }
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        title = "Reminder"
        detail = "10 minutes before"
        onRemove = { removed++ }
        icon = { Box(Modifier.testTag("bell")) }
        onNodeWithTag("bell", useUnmergedTree = true).assertExists()
        onNodeWithText("10 minutes before").assertIsDisplayed()
        onNodeWithContentDescription("Remove Reminder").performClick()
        assertEquals(1, removed)
    }
}
