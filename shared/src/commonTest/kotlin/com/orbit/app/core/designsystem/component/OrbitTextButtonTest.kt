package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitTextButtonTest : ComposeTest() {
    @Test
    fun usesTheControlHeightAndInvokesItsAction() = runComposeUiTest {
        var clicks = 0
        setContent { OrbitTheme { OrbitTextButton("Sign out", onClick = { clicks++ }) } }
        onNodeWithText("Sign out").assertHeightIsEqualTo(56.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun disablesTheActionWhileBusy() = runComposeUiTest {
        setContent { OrbitTheme { OrbitTextButton("Sign out", onClick = {}, enabled = false) } }
        onNodeWithText("Sign out").assertIsNotEnabled()
    }
}
