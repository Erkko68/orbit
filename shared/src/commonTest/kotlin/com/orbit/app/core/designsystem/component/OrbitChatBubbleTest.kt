package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitChatBubbleTest : ComposeTest() {
    @Test
    fun theOtherSideIsOnTheLeftAndTheUserOnTheRight() = runComposeUiTest {
        setContent {
            OrbitTheme {
                Column(Modifier.testTag("chat")) {
                    OrbitChatBubble(mine = false) { Text("Who lives there?") }
                    OrbitChatBubble(mine = true) { Text("Family") }
                }
            }
        }
        val chat = onNodeWithTag("chat").getBoundsInRoot()
        val question = onNodeWithText("Who lives there?").getBoundsInRoot()
        val answer = onNodeWithText("Family").getBoundsInRoot()

        assertTrue(question.left < chat.left + (chat.right - chat.left) / 2)
        assertTrue(answer.right > chat.left + (chat.right - chat.left) / 2)
    }

    @Test
    fun holdsWhatAnswersTheMessage() = runComposeUiTest {
        var tapped = 0
        setContent {
            OrbitTheme {
                OrbitChatBubble(mine = false) {
                    Text("Who lives there?")
                    OrbitFilterChip("Family", selected = true, onClick = { tapped++ })
                }
            }
        }
        onNodeWithText("Family").assertIsSelected().performClick()

        assertEquals(1, tapped)
    }

    @Test
    fun aMessageBeingWrittenStillShowsItsText() = runComposeUiTest {
        setContent { OrbitTheme { OrbitChatBubble(mine = false, loading = true) { Text("Thinking…") } } }

        onNodeWithText("Thinking…").assertIsDisplayed()
    }
}
