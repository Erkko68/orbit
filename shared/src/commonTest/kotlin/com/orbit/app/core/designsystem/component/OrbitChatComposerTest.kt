package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitChatComposerTest : ComposeTest() {
    @Test
    fun sendsOnlyWhenTheCallerAllowsIt() = runComposeUiTest {
        var text by mutableStateOf("")
        var sent = 0
        setContent {
            OrbitTheme {
                OrbitChatComposer(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Answer in your own words",
                    sendDescription = "Send",
                    onSend = { sent++ },
                    canSend = text.isNotBlank(),
                )
            }
        }
        onNodeWithContentDescription("Send").assertIsNotEnabled()

        onNode(hasSetTextAction()).performTextInput("Three of us")
        onNodeWithContentDescription("Send").assertIsEnabled().performClick()

        assertEquals("Three of us", text)
        assertEquals(1, sent)
    }

    @Test
    fun aDisabledFieldTakesNoText() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitChatComposer("", {}, "Answer in your own words", "Send", onSend = {}, enabled = false)
            }
        }
        onNode(hasSetTextAction()).assertDoesNotExist()
    }
}
