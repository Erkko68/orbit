package com.orbit.app.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.attempt
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.repository.AssistantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The assistant's interview, as a chat: once the user consents, one question at a time, each answered with chips,
 * words or both. The words can be dictated: they go after what was already written.
 */
class InterviewViewModel(private val assistant: AssistantRepository) : ViewModel() {
    private val state = MutableStateFlow(InterviewUiState())
    val uiState: StateFlow<InterviewUiState> = state.asStateFlow()

    private var answers: List<InterviewAnswer> = emptyList()

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
        val text = current.text.trim()
        answers = answers + InterviewAnswer(question.id, current.selected, text)
        // What dictation heard is in this answer now: the next one starts empty.
        beforeDictation = ""
        wordsTaken = words(heard).size
        val said = (question.options.filter { it in current.selected } + text).filter(String::isNotEmpty)
        state.update {
            it.copy(
                messages = it.messages + ChatMessage(said.joinToString(", "), mine = true),
                question = null,
                selected = emptySet(),
                text = "",
            )
        }
        ask()
    }

    /** Asks again after a failure that asking again can fix. */
    fun retry() {
        if (state.value.canRetry) ask()
    }

    private fun ask() {
        state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            attempt("Asking the assistant") { assistant.nextStep(answers) }.fold(
                onSuccess = { step ->
                    state.update {
                        when (step) {
                            is InterviewStep.Ask -> it.copy(
                                messages = it.messages + ChatMessage(step.question.text, mine = false),
                                question = step.question,
                                loading = false,
                            )
                            is InterviewStep.PlanReady -> it.copy(loading = false, planReady = true)
                        }
                    }
                },
                onFailure = { failure ->
                    val error = (failure as? AssistantException)?.error ?: AssistantError.UNKNOWN
                    state.update { it.copy(loading = false, error = error) }
                },
            )
        }
    }
}
