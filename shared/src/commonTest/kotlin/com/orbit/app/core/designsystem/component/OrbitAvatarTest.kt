package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitAvatarTest : ComposeTest() {
    @Test
    fun showsInitialsInA36dpCircle() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitAvatar("AL", Color.Red, Modifier.testTag("avatar"))
            }
        }
        onNodeWithText("AL").assertIsDisplayed()
        onNodeWithTag("avatar").assertWidthIsEqualTo(36.dp).assertHeightIsEqualTo(36.dp)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var initials by mutableStateOf("AL")
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitAvatar(initials, accent = Color.Red, modifier = Modifier.testTag("avatar"))
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        initials = "MK"
        onNodeWithText("MK").assertIsDisplayed()
    }
}
