package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertTrue

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

    @Test
    fun showsItsTopBarAboveTheContent() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitScreen(topBar = { OrbitTopBar("Items", Modifier.testTag("bar")) }) { Text("content") }
            }
        }
        val bar = onNodeWithTag("bar").getUnclippedBoundsInRoot()
        val content = onNodeWithText("content").getUnclippedBoundsInRoot()
        assertTrue(content.top >= bar.bottom)
    }

    @Test
    fun scrollsToContentBelowTheFoldAndClearsTheFloatingActionButton() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitScreen(floatingActionButton = { Text("New item") }, scrollable = true) {
                    repeat(60) { Text("row $it") }
                    Text("last")
                }
            }
        }
        // Scrolling to a node stops as soon as it shows: swipe on to the end of the screen.
        onNodeWithText("last").performScrollTo().assertIsDisplayed()
        onRoot().performTouchInput { swipeUp() }
        val button = onNodeWithText("New item").getUnclippedBoundsInRoot()
        val last = onNodeWithText("last").getUnclippedBoundsInRoot()
        assertTrue(last.bottom <= button.top)
    }

    @Test
    fun showsItsFloatingActionButtonBelowTheContent() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitScreen(floatingActionButton = { Text("New item") }) { Text("content") }
            }
        }
        val button = onNodeWithText("New item").getUnclippedBoundsInRoot()
        val content = onNodeWithText("content").getUnclippedBoundsInRoot()
        assertTrue(button.top > content.bottom)
    }
}
