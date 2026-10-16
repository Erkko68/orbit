package com.orbit.app.core.designsystem.component

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitProgressTest : ComposeTest() {
    @Test
    fun showsItsTextsAndAClampedBar() = runComposeUiTest {
        setContent { OrbitTheme { OrbitProgress("Progress", "6 of 9 done", 1.5f) } }
        onNodeWithText("Progress").assertIsDisplayed()
        onNodeWithText("6 of 9 done").assertIsDisplayed()
        onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(1f, 0f..1f))).assertIsDisplayed()
    }
}
