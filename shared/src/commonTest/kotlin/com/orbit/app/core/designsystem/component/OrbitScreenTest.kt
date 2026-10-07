package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitScreenTest : ComposeTest() {
    @Test
    fun showsItsContent() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitScreen { Text("content") }
            }
        }
        onNodeWithText("content").assertIsDisplayed()
    }

    @Test
    fun keepsItsContentWhenTheCallerRecomposes() = runComposeUiTest {
        var alignment by mutableStateOf(Alignment.Start)
        var title by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(title)
                OrbitScreen(Modifier, Arrangement.Top, alignment) { Text("content") }
            }
        }
        onNodeWithText("content").assertIsDisplayed()

        alignment = Alignment.CenterHorizontally
        waitForIdle()
        title = "second"

        onNodeWithText("second").assertIsDisplayed()
        onNodeWithText("content").assertIsDisplayed()
    }
}
