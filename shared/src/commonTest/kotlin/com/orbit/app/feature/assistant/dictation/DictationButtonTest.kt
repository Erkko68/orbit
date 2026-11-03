package com.orbit.app.feature.assistant.dictation

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.testing.FakeDictation
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DictationButtonTest : ComposeTest() {
    private val dictation = FakeDictation()
    private val viewModel = DictationViewModel(dictation)
    private var starts = 0
    private val transcripts = mutableListOf<String>()
    private var prompts = 0

    private fun ComposeUiTest.show() = setContent {
        OrbitTheme {
            DictationButton(
                onStart = { starts++ },
                onTranscript = { transcripts += it },
                viewModel = viewModel,
                prompt = { prompts++ },
            )
        }
    }

    @Test
    fun dictatesIntoTheFieldUntilStopped() = runComposeUiTest {
        show()

        onNodeWithContentDescription("Dictate").performClick()
        runOnIdle { dictation.say("Three of us") }

        onNodeWithContentDescription("Stop dictating").assertIsDisplayed()
        waitForIdle()
        assertEquals(1, starts)
        assertEquals(listOf("Three of us"), transcripts)

        onNodeWithContentDescription("Stop dictating").performClick()
        onNodeWithContentDescription("Dictate").assertIsDisplayed()
    }

    @Test
    fun asksForTheMicrophoneWhenNotAllowed() = runComposeUiTest {
        dictation.allowed = false
        show()

        onNodeWithContentDescription("Dictate").performClick()

        waitForIdle()
        assertEquals(1, prompts)
    }

    @Test
    fun aRefusedMicrophoneIsExplainedAndTypingGoesOn() = runComposeUiTest {
        dictation.allowed = false
        show()
        onNodeWithContentDescription("Dictate").performClick()

        runOnIdle { viewModel.onPermissionAnswer(false) }

        onNodeWithText(
            "Orbit needs the microphone to dictate. You can allow it in the phone settings, or type your answer.",
        ).assertIsDisplayed()
        onNodeWithText("OK").performClick()
        onNodeWithText("Dictation").assertDoesNotExist()
    }

    @Test
    fun aPhoneThatCannotDictateSaysSo() = runComposeUiTest {
        dictation.available = false
        show()

        onNodeWithContentDescription("Dictate").performClick()

        onNodeWithText("This phone cannot dictate in its language. You can type your answer.").assertIsDisplayed()
    }
}
