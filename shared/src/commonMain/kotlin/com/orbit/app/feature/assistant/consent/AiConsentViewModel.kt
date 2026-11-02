package com.orbit.app.feature.assistant.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The AI consent of the signed-in user: settled at once when they accepted before, otherwise asked.
 * Accepting is saved to their profile, so it is asked once per account on any device.
 */
class AiConsentViewModel(private val auth: AuthRepository) : ViewModel() {
    /** What the user did in the notice. [AiConsentUiState.asking] comes from the profile in [uiState]. */
    private val answer = MutableStateFlow(AiConsentUiState())

    val uiState: StateFlow<AiConsentUiState> = combine(
        // Unreadable, the profile counts as not accepted: asking again is the safe side.
        auth.session.orOnFailure("Reading the AI consent", null),
        answer,
    ) { user, answer ->
        when {
            answer.result != null -> AiConsentUiState(result = answer.result)
            user == null -> AiConsentUiState()
            user.aiConsent -> AiConsentUiState(result = true)
            else -> answer.copy(asking = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiConsentUiState())

    fun accept() {
        if (answer.value.submitting) return
        answer.update { it.copy(submitting = true, acceptFailed = false) }
        viewModelScope.launch {
            val saved = attempt("Accepting the AI consent") { auth.acceptAiConsent() }.isSuccess
            answer.update {
                if (saved) AiConsentUiState(result = true) else it.copy(submitting = false, acceptFailed = true)
            }
        }
    }

    /** Goes on by hand, unless the acceptance is being saved. */
    fun decline() = answer.update { if (it.submitting) it else AiConsentUiState(result = false) }

    /** The caller has the result: the next time the assistant opens starts over. */
    fun onResultDelivered() {
        answer.value = AiConsentUiState()
    }
}
