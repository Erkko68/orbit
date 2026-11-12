package com.orbit.app.feature.assistant.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.model.Plan
import com.orbit.app.feature.assistant.InterviewViewModel
import com.orbit.app.testing.FakeDictation
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.ScriptedAssistantRepository
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class InterviewScreenTest : ComposeTest() {
    private val who = InterviewQuestion("q1", "Who lives there?", listOf("Roommates", "Family"), false, true, 1, 8)
    private val rooms = InterviewQuestion("q2", "Which rooms?", listOf("Kitchen", "Bathroom"), true, false, 2, 8)
    private val noPlan = InterviewStep.PlanReady(Plan(space = null, items = emptyList()))
    private val auth = InMemoryAuthRepository().apply {
        runBlocking {
            register("ana@orbit.app", "secret", "Ana López")
            acceptAiConsent()
        }
    }
    private val dictation = FakeDictation()
    private var closed = 0
    private var plans = 0

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun ComposeUiTest.show(assistant: ScriptedAssistantRepository) {
        startTestKoin(auth = auth, spaces = InMemorySpaceRepository(auth), assistant = assistant, dictation = dictation)
        setContent {
            OrbitTheme {
                InterviewScreen(
                    onClose = { closed++ },
                    onPlanReady = { plans++ },
                    viewModel = InterviewViewModel(assistant),
                )
            }
        }
    }

    @Test
    fun answersWithAChipAndTheAnswerJoinsTheChat() = runComposeUiTest {
        show(ScriptedAssistantRepository(listOf(InterviewStep.Ask(who), InterviewStep.Ask(rooms))))

        onNodeWithText("Who lives there?").assertIsDisplayed()
        onNodeWithContentDescription("Send").assertIsNotEnabled()
        onNodeWithText("Family").performClick()
        onNodeWithContentDescription("Send").assertIsEnabled().performClick()

        onNodeWithText("Which rooms?").assertIsDisplayed()
        onNodeWithText("Who lives there?").assertIsDisplayed()
        onNodeWithText("Family").assertIsDisplayed()
        onNodeWithText("Roommates").assertDoesNotExist()
    }

    @Test
    fun deletedTextStaysDeletedWhenAChipIsPicked() = runComposeUiTest {
        show(ScriptedAssistantRepository(listOf(InterviewStep.Ask(who))))

        onNode(hasSetTextAction()).performTextInput("abc")
        onNode(hasSetTextAction()).performTextReplacement("ab")
        onNode(hasSetTextAction()).performTextReplacement("a")
        onNode(hasSetTextAction()).performTextReplacement("")
        onNodeWithText("Family").performClick()
        waitForIdle()

        onNodeWithText("abc").assertDoesNotExist()
        onNodeWithText("a").assertDoesNotExist()
    }

    @Test
    fun deletedDictationStaysDeletedWhileTheMicrophoneListens() = runComposeUiTest {
        show(ScriptedAssistantRepository(listOf(InterviewStep.Ask(who))))

        onNodeWithContentDescription("Dictate").performClick()
        runOnIdle { dictation.say("three of us") }
        onNodeWithText("three of us").assertIsDisplayed()
        onNode(hasSetTextAction()).performTextReplacement("")
        onNodeWithText("Family").performClick()
        runOnIdle { dictation.say("three of us and a cat") }

        onNodeWithText("three of us", substring = true).assertDoesNotExist()
        onNodeWithText("and a cat").assertIsDisplayed()
    }

    @Test
    fun answersInWordsTypedOrDictated() = runComposeUiTest {
        show(ScriptedAssistantRepository(listOf(InterviewStep.Ask(who), noPlan)))

        onNode(hasSetTextAction()).performTextInput("We are")
        onNodeWithContentDescription("Dictate").performClick()
        runOnIdle { dictation.say("three of us") }

        onNodeWithText("We are three of us").assertIsDisplayed()
        onNodeWithContentDescription("Stop dictating").performClick()
        onNodeWithContentDescription("Send").performClick()

        waitForIdle()
        assertEquals(1, plans)
    }

    @Test
    fun aQuestionWithoutFreeTextCannotBeTyped() = runComposeUiTest {
        show(ScriptedAssistantRepository(listOf(InterviewStep.Ask(rooms))))

        onNodeWithText("Which rooms?").assertIsDisplayed()
        onNode(hasSetTextAction()).assertDoesNotExist()
        onNodeWithContentDescription("Dictate").assertDoesNotExist()
    }

    @Test
    fun anAssistantThatIsNotAvailableSaysSoAndOffersToTryAgain() = runComposeUiTest {
        show(
            ScriptedAssistantRepository(
                steps = listOf(InterviewStep.Ask(who)),
                failures = mapOf(0 to AssistantException(AssistantError.UNAVAILABLE)),
            ),
        )

        onNodeWithText("The assistant is not available").assertIsDisplayed()
        onNodeWithText("Try again").performClick()

        onNodeWithText("Who lives there?").assertIsDisplayed()
        onNodeWithText("The assistant is not available").assertDoesNotExist()
    }

    @Test
    fun aRefusalOnlyLeavesClosingTheChat() = runComposeUiTest {
        show(
            ScriptedAssistantRepository(
                steps = emptyList(),
                failures = mapOf(0 to AssistantException(AssistantError.QUOTA_EXCEEDED)),
            ),
        )

        onNodeWithText("That's all for now").assertIsDisplayed()
        onNodeWithText("Try again").assertDoesNotExist()
        onNodeWithContentDescription("Send").assertIsNotEnabled()
        onNodeWithContentDescription("Close").performClick()

        assertEquals(1, closed)
    }

    @Test
    fun decliningTheConsentClosesTheChat() = runComposeUiTest {
        val assistant = ScriptedAssistantRepository(listOf(InterviewStep.Ask(who)))
        val fresh = InMemoryAuthRepository().apply { runBlocking { register("bo@orbit.app", "secret", "Bo") } }
        startTestKoin(auth = fresh, spaces = InMemorySpaceRepository(fresh), assistant = assistant, dictation = dictation)
        setContent {
            OrbitTheme {
                InterviewScreen(onClose = { closed++ }, onPlanReady = {}, viewModel = InterviewViewModel(assistant))
            }
        }

        onNodeWithText("Set up by hand").performClick()

        waitForIdle()
        assertEquals(1, closed)
        assertEquals(0, assistant.calls.size)
    }
}
