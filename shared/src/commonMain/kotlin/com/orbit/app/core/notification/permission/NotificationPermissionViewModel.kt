package com.orbit.app.core.notification.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.notification.NotificationAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Gets notifications allowed: checks, shows the system prompt once, then offers the settings if refused. */
class NotificationPermissionViewModel(private val access: NotificationAccess) : ViewModel() {
    private val state = MutableStateFlow(NotificationPermissionUiState())
    val uiState: StateFlow<NotificationPermissionUiState> = state.asStateFlow()

    /** The prompt is open, so coming back to the screen means nothing until it answers. */
    private var awaitingAnswer = false

    /** The screen is in front: shown for the first time, or back from the prompt or the system settings. */
    fun onResume() {
        val current = state.value
        if (awaitingAnswer || current.prompting || current.result != null) return
        viewModelScope.launch {
            when {
                access.isAllowed() -> finish(true)
                !current.refused -> state.update { it.copy(prompting = true) }
            }
        }
    }

    fun onPromptShown() {
        awaitingAnswer = true
        state.update { it.copy(prompting = false) }
    }

    fun onAnswer(allowed: Boolean) {
        awaitingAnswer = false
        if (allowed) finish(true) else state.update { it.copy(prompting = false, refused = true) }
    }

    fun openSettings() = access.openSettings()

    fun dismiss() = finish(false)

    /** The caller has the result: the next request starts over. */
    fun onResultDelivered() {
        awaitingAnswer = false
        state.value = NotificationPermissionUiState()
    }

    private fun finish(allowed: Boolean) {
        state.value = NotificationPermissionUiState(result = allowed)
    }
}
