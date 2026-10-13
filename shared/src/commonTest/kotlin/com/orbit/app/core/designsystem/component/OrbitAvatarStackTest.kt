package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitAvatarStackTest : ComposeTest() {
    @Test
    fun showsEveryoneWhenTheyFit() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitAvatarStack(listOf("AL", "MK"), max = 2)
            }
        }
        onNodeWithText("AL").assertIsDisplayed()
        onNodeWithText("MK").assertIsDisplayed()
        onNodeWithText("+", substring = true).assertDoesNotExist()
    }

    @Test
    fun countsTheOnesPastTheMaximum() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitAvatarStack(listOf("AL", "MK", "SJ", "DT", "PM"), max = 2)
            }
        }
        onNodeWithText("MK").assertIsDisplayed()
        onNodeWithText("SJ").assertDoesNotExist()
        onNodeWithText("+3").assertIsDisplayed()
    }
}
