package com.orbit.app.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.PlanDraft
import com.orbit.app.core.attempt
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.repository.AssistantRepository
import com.orbit.app.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The assistant's interview, as a chat: once the user consents, one question at a time, each answered with chips,
 * words or both. The words can be dictated: they go after what was already written. A plan goes to the [plan] draft,
 * which the review board shows (#75). A removal request ends in a proposal to delete items instead (#89): all ticked,
 * the user unticks what stays, and confirming deletes the rest in one batch. The interview is about [spaceId], or a
 * new space when it is empty.
 */
class InterviewViewModel(
    private val spaceId: String,
    private val assistant: AssistantRepository,
    private val plan: PlanDraft,
    private val items: ItemRepository,
) : ViewModel() {
    private val state = MutableStateFlow(InterviewUiState())
    val uiState: StateFlow<InterviewUiState> = state.asStateFlow()

    private var answers: List<InterviewAnswer> = emptyList()

    /** The questions of [answers], in the same order: Back returns to the last one. */
    private var asked: List<InterviewQuestion> = emptyList()

    /** What was written when the current dictation began, or last edited by hand: what is heard goes after it. */
    private var beforeDictation = ""

    /** Everything the current dictation has heard. The recogniser sends it all again with each new word. */
    private var heard = ""

    /** How many of the [heard] words were there at the last edit by hand: they are written, or deleted, already. */
    private var wordsTaken = 0

    /** The user accepted the consent notice: the interview starts. */
    fun onConsent() {
        if (state.value.consented) return
        state.update { it.copy(consented = true) }
        ask()
    }

    fun select(option: String) = state.update { current ->
        val question = current.question ?: return@update current
        val selected = when {
            option in current.selected -> current.selected - option
            question.multiSelect -> current.selected + option
            else -> setOf(option)
        }
        current.copy(selected = selected)
    }

    /** The user typed or deleted: while dictating, only what is heard after this goes after it. */
    fun onTextChange(text: String) {
        beforeDictation = text.trimEnd()
        wordsTaken = words(heard).size
        state.update { it.copy(text = text) }
    }

    fun onDictationStart() {
        beforeDictation = state.value.text.trimEnd()
        heard = ""
        wordsTaken = 0
    }

    fun onDictation(heard: String) {
        this.heard = heard
        val new = words(heard).drop(wordsTaken).joinToString(" ")
        state.update { it.copy(text = listOf(beforeDictation, new).filter(String::isNotBlank).joinToString(" ")) }
    }

    private fun words(text: String) = text.split(' ').filter(String::isNotBlank)

    /** Sends the answer and asks for the next step. */
    fun send() {
        val current = state.value
        val question = current.question ?: return
        if (!current.canSend) return
        answer(question, InterviewAnswer(question.id, question.text, current.selected, current.text.trim()))
    }

    /** Leaves the question unanswered and moves on (#74). */
    fun skip() {
        val question = state.value.question ?: return
        if (!state.value.canAct) return
        answer(question, InterviewAnswer(question.id, question.text, emptySet(), ""))
    }

    /** Answers with what the assistant suggested, and shows it as the answer (#74). */
    fun decide() {
        val question = state.value.question ?: return
        val suggestion = question.suggestion ?: return
        if (!state.value.canAct) return
        val chosen = suggestion in question.options
        val answer = InterviewAnswer(
            question.id, question.text,
            selected = if (chosen) setOf(suggestion) else emptySet(),
            text = if (chosen) "" else suggestion,
            decided = true,
        )
        answer(question, answer)
    }

    /** Returns to the previous question, with its answer as it was, so it can be changed (#74). */
    fun back() {
        if (!state.value.canGoBack) return
        val previous = asked.last()
        val answer = answers.last()
        asked = asked.dropLast(1)
        answers = answers.dropLast(1)
        startFresh()
        state.update {
            it.copy(
                messages = chat(previous),
                question = previous,
                selected = answer.selected,
                text = answer.text,
                answered = answers.size,
                error = null,
            )
        }
    }

    private fun answer(question: InterviewQuestion, answer: InterviewAnswer) {
        asked = asked + question
        answers = answers + answer
        startFresh()
        state.update {
            it.copy(messages = chat(null), question = null, selected = emptySet(), text = "", answered = answers.size)
        }
        ask()
    }

    /** What dictation heard is in an answer now: the next one starts empty. */
    private fun startFresh() {
        beforeDictation = ""
        wordsTaken = words(heard).size
    }

    /** The chat for the questions asked and their answers, then [current] when one is waiting. */
    private fun chat(current: InterviewQuestion?): List<ChatMessage> =
        asked.zip(answers).flatMap { (question, answer) ->
            val said = (question.options.filter { it in answer.selected } + answer.text).filter(String::isNotEmpty)
            listOf(
                ChatMessage(question.text, mine = false),
                ChatMessage(said.joinToString(", "), mine = true, decided = answer.decided),
            )
        } + listOfNotNull(current?.let { ChatMessage(it.text, mine = false) })

    /** Ticks or unticks a proposed removal: an unticked item stays. */
    fun toggleRemoval(itemId: String) = state.update { current ->
        val rows = current.removal ?: return@update current
        current.copy(
            removal = rows.map { if (it.itemId == itemId) it.copy(ticked = !it.ticked) else it },
            deleteFailed = false,
        )
    }

    /** Nothing is deleted: back to the request, as it was written, so it can be put another way. */
    fun dismissRemoval() {
        if (state.value.removal == null || state.value.deleting) return
        state.update { it.copy(removal = null, deleteFailed = false) }
        back()
    }

    /** Deletes the ticked items, all in one batch. */
    fun confirmRemoval() {
        val current = state.value
        if (!current.canDelete) return
        val ids = current.removal.orEmpty().filter { it.ticked }.map { it.itemId }
        state.update { it.copy(deleting = true, deleteFailed = false) }
        viewModelScope.launch {
            val deleted = attempt("Deleting what the assistant proposed") { items.deleteProposed(spaceId, ids) }.isSuccess
            state.update { it.copy(deleting = false, deleteFailed = !deleted, removed = deleted) }
        }
    }

    /** Asks again after a failure that asking again can fix. */
    fun retry() {
        if (state.value.canRetry) ask()
    }

    private fun ask() {
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            attempt("Asking the assistant") { assistant.nextStep(spaceId, answers) }.fold(
                onSuccess = { step ->
                    state.update {
                        when (step) {
                            is InterviewStep.Ask -> it.copy(
                                messages = chat(step.question),
                                question = step.question,
                                loading = false,
                            )
                            is InterviewStep.PlanReady -> {
                                plan.propose(step.plan)
                                it.copy(loading = false, planReady = true)
                            }
                            is InterviewStep.RemovalReady -> it.copy(
                                loading = false,
                                removal = step.removals.map { removal ->
                                    RemovalRow(removal.itemId, removal.title, removal.reason)
                                },
                            )
                        }
                    }
                },
                onFailure = { failure ->
                    val refusal = failure as? AssistantException
                    val resetsOn = refusal?.resetsAt?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
                    state.update {
                        it.copy(loading = false, error = refusal?.error ?: AssistantError.UNKNOWN, quotaResetsOn = resetsOn)
                    }
                },
            )
        }
    }
}
