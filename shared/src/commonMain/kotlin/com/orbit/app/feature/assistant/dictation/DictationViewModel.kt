package com.orbit.app.feature.assistant.dictation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.orbit.app.core.speech.Dictation
import com.orbit.app.core.speech.DictationException
import com.orbit.app.core.speech.DictationFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Dictates into a text answer: checks that the phone can, asks for the microphone once, then listens. What it
 * hears is [DictationUiState.transcript]; the field it belongs to decides where that text goes.
 */
class DictationViewModel(private val dictation: Dictation) : ViewModel() {
    private val state = MutableStateFlow(DictationUiState())
    val uiState: StateFlow<DictationUiState> = state.asStateFlow()

    private var listening: Job? = null

    /** The microphone button: starts dictating, or stops it while listening. */
    fun toggle() {
        if (state.value.listening) {
            listening?.cancel()
            return
        }
        if (!dictation.isAvailable()) {
            state.update { it.copy(failure = DictationFailure.UNAVAILABLE) }
            return
        }
        viewModelScope.launch {
            if (dictation.hasPermission()) listen() else state.update { it.copy(askingPermission = true) }
        }
    }

    fun onPermissionPromptShown() = state.update { it.copy(askingPermission = false) }

    fun onPermissionAnswer(allowed: Boolean) {
        if (allowed) listen() else state.update { it.copy(failure = DictationFailure.NO_PERMISSION) }
    }

    fun dismissFailure() = state.update { it.copy(failure = null) }

    private fun listen() {
        state.update { it.copy(listening = true, started = it.started + 1, transcript = "", failure = null) }
        listening = viewModelScope.launch {
            try {
                dictation.listen().collect { heard -> state.update { it.copy(transcript = heard) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: DictationException) {
                state.update { it.copy(failure = e.failure) }
            } catch (e: Exception) {
                Logger.e(e) { "Dictation failed" }
                state.update { it.copy(failure = DictationFailure.ERROR) }
            } finally {
                state.update { it.copy(listening = false) }
            }
        }
    }
}
