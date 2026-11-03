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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
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
    private val auth = InMemoryAuthRepository().apply {
        runBlocking {
            register("ana@orbit.app", "secret", "Ana López")
            acceptAiConsent()
        }
    }
    private val dictation = FakeDictation()
    private var manual = 0
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
                    onClose = {},
                    onManual = { manual++ },
                    onPlanReady = { plans++ },
                    viewModel = InterviewViewModel(assistant),
                )
            }
        }
    }

    @Test
    fun answersWithAChipAndMovesOn() = runComposeUiTest {
        show(ScriptedAssistantRepository(listOf(InterviewStep.Ask(who), InterviewStep.Ask(rooms))))

        onNodeWithText("Who lives there?").assertIsDisplayed()
        onNodeWithText("Send").performScrollTo().assertIsNotEnabled()
        onNodeWithText("Family").performScrollTo().performClick()
        onNodeWithText("Send").performScrollTo().assertIsEnabled().performClick()

        onNodeWithText("Which rooms?").assertIsDisplayed()
    }

    @Test
    fun answersInWordsTypedOrDictated() = runComposeUiTest {
        show(ScriptedAssistantRepository(listOf(InterviewStep.Ask(who), InterviewStep.PlanReady)))

        onNode(hasSetTextAction()).performScrollTo().performTextInput("We are")
        onNodeWithContentDescription("Dictate").performScrollTo().performClick()
        runOnIdle { dictation.say("three of us") }

        onNodeWithText("We are three of us").assertIsDisplayed()
        onNodeWithContentDescription("Stop dictating").performClick()
        onNodeWithText("Send").performScrollTo().performClick()

        waitForIdle()
        assertEquals(1, plans)
    }

    @Test
    fun aQuestionWithoutFreeTextHasNoField() = runComposeUiTest {
        show(ScriptedAssistantRepository(listOf(InterviewStep.Ask(rooms))))

        onNodeWithText("Which rooms?").assertIsDisplayed()
        onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun aFailureThatCanPassOffersToTryAgain() = runComposeUiTest {
        show(
            ScriptedAssistantRepository(
                steps = listOf(InterviewStep.Ask(who)),
                failures = mapOf(0 to AssistantException(AssistantError.UNAVAILABLE)),
            ),
        )

        onNodeWithText("The assistant is not answering right now. Try again, or set up by hand.").assertIsDisplayed()
        onNodeWithText("Try again").performScrollTo().performClick()

        onNodeWithText("Who lives there?").assertIsDisplayed()
    }

    @Test
    fun aRefusalOnlyLeavesTheManualPath() = runComposeUiTest {
        show(
            ScriptedAssistantRepository(
                steps = emptyList(),
                failures = mapOf(0 to AssistantException(AssistantError.QUOTA_EXCEEDED)),
            ),
        )

        onNodeWithText("You have used the assistant all you can for now. You can set up by hand.").assertIsDisplayed()
        onNodeWithText("Try again").assertDoesNotExist()
        onNodeWithText("Set up by hand instead").performScrollTo().performClick()

        assertEquals(1, manual)
    }

    @Test
    fun decliningTheConsentIsTheManualPath() = runComposeUiTest {
        val assistant = ScriptedAssistantRepository(listOf(InterviewStep.Ask(who)))
        val fresh = InMemoryAuthRepository().apply { runBlocking { register("bo@orbit.app", "secret", "Bo") } }
        startTestKoin(auth = fresh, spaces = InMemorySpaceRepository(fresh), assistant = assistant, dictation = dictation)
        setContent {
            OrbitTheme {
                InterviewScreen(onClose = {}, onManual = { manual++ }, onPlanReady = {}, viewModel = InterviewViewModel(assistant))
            }
        }

        onNodeWithText("Set up by hand").performClick()

        waitForIdle()
        assertEquals(1, manual)
        assertEquals(0, assistant.calls.size)
    }
}
