package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.designsystem.ComposeTest
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_check
import orbit.shared.generated.resources.ic_undo
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitHeaderActionTest : ComposeTest() {
    @Test
    fun showsItsTextAndClicks() = runComposeUiTest {
        var clicks = 0
        setContent { OrbitTheme { OrbitHeaderAction("Mark as done", Res.drawable.ic_check, { clicks++ }) } }

        onNodeWithText("Mark as done").assertIsDisplayed().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun ignoresClicksWhenDisabled() = runComposeUiTest {
        var clicks = 0
        setContent { OrbitTheme { OrbitHeaderAction("Reopen", Res.drawable.ic_undo, { clicks++ }, enabled = false, quiet = true) } }

        onNodeWithText("Reopen").assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }
}
