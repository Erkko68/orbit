package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitDetailsFieldTest : ComposeTest() {
    @Test
    fun saysWhatGoesThereAndPassesOnWhatIsTyped() = runComposeUiTest {
        var typed = ""
        setContent { OrbitTheme { OrbitDetailsField("", { typed = it }, "Write the details here", {}) } }

        onNodeWithText("Write the details here").assertIsDisplayed()
        onNodeWithContentDescription("Write the details here").performTextInput("Under the sink")

        assertEquals("Under the sink", typed)
    }

    @Test
    fun cannotBeTypedInWhenOff() = runComposeUiTest {
        setContent { OrbitTheme { OrbitDetailsField("", {}, "Write the details here", {}, enabled = false) } }

        onNodeWithContentDescription("Write the details here").assertIsNotEnabled()
    }
}
