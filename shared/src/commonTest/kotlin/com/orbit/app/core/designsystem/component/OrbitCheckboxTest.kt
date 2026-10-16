package com.orbit.app.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.assertEquals
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitCheckboxTest : ComposeTest() {
    @Test
    fun readOnlyCheckboxExposesItsStateWithoutAnAction() = runComposeUiTest {
        setContent { OrbitTheme { OrbitCheckbox(true, null, Modifier.semantics { contentDescription = "Milk" }) } }
        onNodeWithContentDescription("Milk").assertIsOn().assertHasNoClickAction()
    }

    @Test
    fun disabledCheckboxCannotStartAnotherWrite() = runComposeUiTest {
        var calls = 0
        setContent {
            OrbitTheme {
                OrbitCheckbox(
                    true,
                    { calls++ },
                    Modifier.semantics { contentDescription = "Task" },
                    enabled = false
                )
            }
        }
        onNodeWithContentDescription("Task").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test
    fun displaysItsStateAndForwardsTheToggle() = runComposeUiTest {
        var next: Boolean? = null
        setContent {
            OrbitTheme {
                OrbitCheckbox(
                    true,
                    { next = it },
                    Modifier.semantics { contentDescription = "Task" })
            }
        }
        onNodeWithContentDescription("Task").assertIsOn()
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(false, next)
    }
}
