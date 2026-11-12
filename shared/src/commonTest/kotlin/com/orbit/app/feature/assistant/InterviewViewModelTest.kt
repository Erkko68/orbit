package com.orbit.app.feature.assistant

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.model.Plan
import com.orbit.app.testing.ScriptedAssistantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InterviewViewModelTest {
    private val who = question("q1", "Who lives there?", listOf("Roommates", "Family"))
    private val rooms = question("q2", "Which rooms?", listOf("Kitchen", "Bathroom"), multiSelect = true)
    private val noPlan = InterviewStep.PlanReady(Plan(space = null, items = emptyList()))

    private fun question(id: String, text: String, options: List<String>, multiSelect: Boolean = false) =
        InterviewQuestion(id, text, options, multiSelect, freeText = true, position = 1, budget = 8)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun interview(steps: List<InterviewStep>, failures: Map<Int, Exception> = emptyMap()) =
        ScriptedAssistantRepository(steps, failures).let { it to InterviewViewModel(it) }

    private fun started(steps: List<InterviewStep>, failures: Map<Int, Exception> = emptyMap()) =
        interview(steps, failures).also { (_, viewModel) -> viewModel.onConsent() }

    @Test
    fun asksNothingBeforeConsent() {
        val (assistant, viewModel) = interview(listOf(InterviewStep.Ask(who)))

        assertFalse(viewModel.uiState.value.consented)
        assertTrue(assistant.calls.isEmpty())
    }

    @Test
    fun consentStartsTheInterviewOnce() {
        val (assistant, viewModel) = started(listOf(InterviewStep.Ask(who)))

        viewModel.onConsent()

        val state = viewModel.uiState.value
        assertTrue(state.consented)
        assertEquals(who, state.question)
        assertFalse(state.loading)
        assertFalse(state.canSend)
        assertEquals(1, assistant.calls.size)
    }

    @Test
    fun aSingleChoiceReplacesTheSelectionAndTappingAgainClearsIt() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(who)))

        viewModel.select("Roommates")
        viewModel.select("Family")
        assertEquals(setOf("Family"), viewModel.uiState.value.selected)

        viewModel.select("Family")
        assertTrue(viewModel.uiState.value.selected.isEmpty())
    }

    @Test
    fun aMultipleChoiceTogglesInTheSelection() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(rooms)))

        viewModel.select("Kitchen")
        viewModel.select("Bathroom")
        viewModel.select("Kitchen")

        assertEquals(setOf("Bathroom"), viewModel.uiState.value.selected)
    }

    @Test
    fun dictationGoesAfterWhatWasTyped() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(who)))
        viewModel.onTextChange("We are ")

        viewModel.onDictationStart()
        viewModel.onDictation("three")
        viewModel.onDictation("three of us")

        assertEquals("We are three of us", viewModel.uiState.value.text)
        assertTrue(viewModel.uiState.value.canSend)
    }

    @Test
    fun whatIsDeletedWhileDictatingStaysDeleted() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(who)))
        viewModel.onDictationStart()
        viewModel.onDictation("three of us")

        viewModel.onTextChange("")
        viewModel.onDictation("three of us")
        assertEquals("", viewModel.uiState.value.text)

        viewModel.onDictation("three of us and a cat")
        assertEquals("and a cat", viewModel.uiState.value.text)
    }

    @Test
    fun whatIsTypedWhileDictatingKeepsWhatIsHeardAfterIt() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(who)))
        viewModel.onDictationStart()
        viewModel.onDictation("three of")

        viewModel.onTextChange("Three of")
        viewModel.onDictation("three of us")

        assertEquals("Three of us", viewModel.uiState.value.text)
    }

    @Test
    fun anAnswerSentKeepsItsDictationOutOfTheNext() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(who), InterviewStep.Ask(rooms)))
        viewModel.onDictationStart()
        viewModel.onDictation("three of us")
        viewModel.send()

        viewModel.onDictation("three of us")
        assertEquals("", viewModel.uiState.value.text)

        viewModel.onDictation("three of us kitchen")
        assertEquals("kitchen", viewModel.uiState.value.text)
    }

    @Test
    fun sendingAsksForTheNextStepWithEveryAnswerSoFar() {
        val (assistant, viewModel) = started(listOf(InterviewStep.Ask(who), InterviewStep.Ask(rooms)))
        viewModel.select("Roommates")
        viewModel.onTextChange("  Three of us ")

        viewModel.send()

        val state = viewModel.uiState.value
        assertEquals(rooms, state.question)
        assertTrue(state.selected.isEmpty())
        assertEquals("", state.text)
        assertEquals(listOf(InterviewAnswer("q1", setOf("Roommates"), "Three of us")), assistant.calls[1])
        assertEquals(
            listOf(
                ChatMessage("Who lives there?", mine = false),
                ChatMessage("Roommates, Three of us", mine = true),
                ChatMessage("Which rooms?", mine = false),
            ),
            state.messages,
        )
    }

    @Test
    fun theAnswerKeepsTheOrderOfTheChips() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(rooms), noPlan))
        viewModel.select("Bathroom")
        viewModel.select("Kitchen")

        viewModel.send()

        assertEquals(ChatMessage("Kitchen, Bathroom", mine = true), viewModel.uiState.value.messages.last())
    }

    @Test
    fun onlyAQuestionWithFreeTextCanBeTyped() {
        val (_, chips) = started(listOf(InterviewStep.Ask(rooms.copy(freeText = false))))
        val (_, words) = started(listOf(InterviewStep.Ask(who)))

        assertFalse(chips.uiState.value.canType)
        assertTrue(words.uiState.value.canType)
    }

    @Test
    fun sendingWithoutAnAnswerDoesNothing() {
        val (assistant, viewModel) = started(listOf(InterviewStep.Ask(who)))

        viewModel.send()

        assertEquals(1, assistant.calls.size)
    }

    @Test
    fun aPlanOpensTheReview() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(who), noPlan))
        viewModel.select("Family")

        viewModel.send()

        assertTrue(viewModel.uiState.value.planReady)
        assertFalse(viewModel.uiState.value.loading)
    }

    @Test
    fun aFailureThatCanPassIsRetriedWithTheSameAnswers() {
        val (assistant, viewModel) = started(
            steps = listOf(InterviewStep.Ask(who), InterviewStep.Ask(rooms)),
            failures = mapOf(1 to AssistantException(AssistantError.NETWORK)),
        )
        viewModel.select("Family")
        viewModel.send()

        assertEquals(AssistantError.NETWORK, viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.canRetry)
        assertFalse(viewModel.uiState.value.canSend)
        assertNull(viewModel.uiState.value.question)
        assertEquals(ChatMessage("Family", mine = true), viewModel.uiState.value.messages.last())

        viewModel.retry()

        assertNull(viewModel.uiState.value.error)
        assertEquals(rooms, viewModel.uiState.value.question)
        assertEquals(assistant.calls[1], assistant.calls[2])
    }

    @Test
    fun anUnexpectedFailureReadsAsUnknown() {
        val (_, viewModel) = started(emptyList(), mapOf(0 to IllegalStateException("boom")))

        assertEquals(AssistantError.UNKNOWN, viewModel.uiState.value.error)
    }

    @Test
    fun aRefusalCannotBeRetried() {
        val (quotaAssistant, quota) = started(emptyList(), mapOf(0 to AssistantException(AssistantError.QUOTA_EXCEEDED)))
        val (offAssistant, off) = started(emptyList(), mapOf(0 to AssistantException(AssistantError.DISABLED)))

        quota.retry()
        off.retry()

        assertFalse(quota.uiState.value.canRetry)
        assertFalse(off.uiState.value.canRetry)
        assertEquals(1, quotaAssistant.calls.size)
        assertEquals(1, offAssistant.calls.size)
    }
}
