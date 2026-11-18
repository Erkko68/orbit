package com.orbit.app.feature.assistant

import com.orbit.app.core.PlanDraft
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.PlanReview
import com.orbit.app.testing.ScriptedAssistantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class InterviewViewModelTest {
    private val who = question("q1", "Who lives there?", listOf("Roommates", "Family"))
    private val rooms = question("q2", "Which rooms?", listOf("Kitchen", "Bathroom"), multiSelect = true)
    private val noPlan = InterviewStep.PlanReady(Plan(space = null, items = emptyList()))
    private val draft = PlanDraft()

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
        ScriptedAssistantRepository(steps, failures).let { it to InterviewViewModel("space-1", it, draft) }

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
        assertEquals(listOf(InterviewAnswer("q1", "Who lives there?", setOf("Roommates"), "Three of us")), assistant.calls[1])
        assertEquals(listOf("space-1", "space-1"), assistant.spaceIds)
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
        val bins = InterviewStep.PlanReady(Plan(space = null, items = listOf(NewItem("Bins"))))
        val (_, viewModel) = started(listOf(InterviewStep.Ask(who), bins))
        viewModel.select("Family")

        viewModel.send()

        assertTrue(viewModel.uiState.value.planReady)
        assertEquals(PlanReview.of(bins.plan), draft.review.value)
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
    fun aSpentQuotaSaysWhichDayItIsBack() {
        val resetsAt = Instant.parse("2026-11-18T00:00:00Z")
        val refusal = AssistantException(AssistantError.QUOTA_EXCEEDED, resetsAt = resetsAt)

        val (_, viewModel) = started(emptyList(), mapOf(0 to refusal))

        assertEquals(resetsAt.toLocalDateTime(TimeZone.currentSystemDefault()).date, viewModel.uiState.value.quotaResetsOn)
    }

    @Test
    fun skipLeavesTheQuestionUnansweredAndMovesOn() {
        val (assistant, viewModel) = started(listOf(InterviewStep.Ask(who), InterviewStep.Ask(rooms)))

        viewModel.skip()

        assertEquals(InterviewAnswer("q1", "Who lives there?", emptySet(), ""), assistant.calls[1].single())
        val state = viewModel.uiState.value
        assertEquals(rooms, state.question)
        assertTrue(state.messages[1].skipped)
    }

    @Test
    fun decideAnswersWithTheSuggestionAndShowsIt() {
        val suggested = who.copy(suggestion = "Family")
        val (assistant, viewModel) = started(listOf(InterviewStep.Ask(suggested), InterviewStep.Ask(rooms)))

        assertTrue(viewModel.uiState.value.canDecide)
        viewModel.decide()

        assertEquals(InterviewAnswer("q1", "Who lives there?", setOf("Family"), "", decided = true), assistant.calls[1].single())
        assertEquals(ChatMessage("Family", mine = true, decided = true), viewModel.uiState.value.messages[1])
    }

    @Test
    fun aSuggestionInWordsIsWrittenAsTheAnswer() {
        val words = who.copy(options = emptyList(), suggestion = "Three of us")
        val (assistant, viewModel) = started(listOf(InterviewStep.Ask(words), InterviewStep.Ask(rooms)))

        viewModel.decide()

        assertEquals(InterviewAnswer("q1", "Who lives there?", emptySet(), "Three of us", decided = true), assistant.calls[1].single())
    }

    @Test
    fun withoutASuggestionTheAssistantCannotDecide() {
        val (assistant, viewModel) = started(listOf(InterviewStep.Ask(who)))

        assertFalse(viewModel.uiState.value.canDecide)
        viewModel.decide()

        assertEquals(1, assistant.calls.size)
    }

    @Test
    fun backReturnsToThePreviousQuestionWithItsAnswerKept() {
        val (assistant, viewModel) = started(listOf(InterviewStep.Ask(who), InterviewStep.Ask(rooms), InterviewStep.Ask(rooms)))
        assertFalse(viewModel.uiState.value.canGoBack)
        viewModel.select("Family")
        viewModel.onTextChange("two kids")
        viewModel.send()

        viewModel.back()

        val state = viewModel.uiState.value
        assertEquals(who, state.question)
        assertEquals(setOf("Family"), state.selected)
        assertEquals("two kids", state.text)
        assertEquals(listOf(ChatMessage("Who lives there?", mine = false)), state.messages)
        assertFalse(state.canGoBack)
        assertEquals(2, assistant.calls.size)

        viewModel.select("Roommates")
        viewModel.send()
        assertEquals(setOf("Roommates"), assistant.calls[2].single().selected)
    }

    @Test
    fun backAfterAFailureReturnsToTheQuestionThatFailed() {
        val (_, viewModel) = started(listOf(InterviewStep.Ask(who)), mapOf(1 to AssistantException(AssistantError.NETWORK)))
        viewModel.select("Family")
        viewModel.send()
        assertEquals(AssistantError.NETWORK, viewModel.uiState.value.error)

        viewModel.back()

        assertNull(viewModel.uiState.value.error)
        assertEquals(who, viewModel.uiState.value.question)
        assertEquals(setOf("Family"), viewModel.uiState.value.selected)
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
