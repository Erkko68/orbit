package com.orbit.app.core.designsystem.component

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitProgressCardTest : ComposeTest() {
    @Test
    fun exposesTheLabelAndAccessibleFraction() = runComposeUiTest {
        setContent { OrbitTheme { OrbitProgressCard("2 of 6 checked", 2f / 6) } }
        onNodeWithText("2 of 6 checked").assertIsDisplayed()
        onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(2f / 6, 0f..1f)
            )
        ).assertIsDisplayed()
    }
}
