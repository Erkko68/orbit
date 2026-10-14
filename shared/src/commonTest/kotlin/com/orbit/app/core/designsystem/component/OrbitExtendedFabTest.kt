package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_add

@OptIn(ExperimentalTestApi::class)
class OrbitExtendedFabTest : ComposeTest() {
    @Test
    fun showsItsIconAndTextAndClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitExtendedFab("New item", icon = Res.drawable.ic_add, onClick = { clicks++ })
            }
        }
        onNodeWithText("New item").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun followsItsInputsWhenTheCallerRecomposes() = runComposeUiTest {
        var text by mutableStateOf("New item")
        var other by mutableStateOf("first")
        setContent {
            OrbitTheme {
                Text(other)
                OrbitExtendedFab(text, icon = Res.drawable.ic_add, onClick = {}, modifier = Modifier.testTag("fab"))
            }
        }
        other = "second"
        onNodeWithText("second").assertIsDisplayed()

        text = "New list"
        onNodeWithText("New list").assertIsDisplayed()
    }
}
