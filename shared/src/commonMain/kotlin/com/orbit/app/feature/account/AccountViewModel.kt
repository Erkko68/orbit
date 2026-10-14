package com.orbit.app.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(private val auth: AuthRepository) : ViewModel() {
    // Null fields follow the repository until edited. Drafts belong to one user only.
    private val form = MutableStateFlow(AccountForm())

    val uiState: StateFlow<AccountUiState> = combine(auth.session, form) { user, draft ->
        val current = if (draft.userId == user?.id) draft else AccountForm()
        AccountUiState(
            user = user,
            displayName = current.displayName ?: user?.displayName.orEmpty(),
            language = current.language ?: user?.language ?: "en",
            timeZone = current.timeZone ?: user?.timeZone?.id.orEmpty(),
            timeZonePickerOpen = current.timeZonePickerOpen,
            timeZoneQuery = current.timeZoneQuery,
            saving = current.saving,
            signingOut = current.signingOut,
            saveFailed = current.saveFailed,
            signOutFailed = current.signOutFailed,
            saved = current.saved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun onDisplayNameChange(name: String) = edit { it.copy(displayName = name) }

    fun selectLanguage(language: String) {
        if (language in listOf("es", "ca", "en")) edit { it.copy(language = language) }
    }

    fun onTimeZoneChange(timeZone: String) = edit {
        it.copy(timeZone = timeZone, timeZonePickerOpen = false, timeZoneQuery = "")
    }

    fun openTimeZonePicker() = edit(clearResult = false) { it.copy(timeZonePickerOpen = true, timeZoneQuery = "") }

    fun onTimeZoneQueryChange(query: String) = edit(clearResult = false) { it.copy(timeZoneQuery = query) }

    fun closeTimeZonePicker() = edit(clearResult = false) { it.copy(timeZonePickerOpen = false, timeZoneQuery = "") }

    fun saveProfile() {
        val state = uiState.value
        if (!state.canSave || form.value.busy) return
        val timeZone = state.parsedTimeZone ?: return
        val userId = state.user?.id ?: return
        form.update {
            it.copy(
                userId = userId,
                saving = true,
                saveFailed = false,
                signOutFailed = false,
                saved = false
            )
        }
        viewModelScope.launch {
            try {
                auth.updateProfile(state.displayName.trim(), state.language, timeZone)
                form.value = AccountForm(userId = userId, saved = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Updating the account profile failed" }
                form.update { it.copy(saving = false, saveFailed = true) }
            } finally {
                form.update { it.copy(saving = false) }
            }
        }
    }

    fun signOut() {
        val state = uiState.value
        if (!state.canEdit || form.value.busy) return
        val userId = state.user?.id ?: return
        form.update {
            it.copy(
                userId = userId,
                signingOut = true,
                saveFailed = false,
                signOutFailed = false,
                saved = false
            )
        }
        viewModelScope.launch {
            try {
                auth.signOut()
                form.value = AccountForm()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Signing out failed" }
                form.update { it.copy(signingOut = false, signOutFailed = true) }
            } finally {
                form.update { it.copy(signingOut = false) }
            }
        }
    }

    private fun edit(clearResult: Boolean = true, change: (AccountForm) -> AccountForm) {
        val state = uiState.value
        if (!state.canEdit || form.value.busy) return
        val userId = state.user?.id ?: return
        form.update { draft ->
            val current = if (draft.userId == userId) draft else AccountForm(userId = userId)
            val edited = change(current)
            if (clearResult) edited.copy(saveFailed = false, signOutFailed = false, saved = false) else edited
        }
    }

    private data class AccountForm(
        val userId: String? = null,
        val displayName: String? = null,
        val language: String? = null,
        val timeZone: String? = null,
        val timeZonePickerOpen: Boolean = false,
        val timeZoneQuery: String = "",
        val saving: Boolean = false,
        val signingOut: Boolean = false,
        val saveFailed: Boolean = false,
        val signOutFailed: Boolean = false,
        val saved: Boolean = false,
    ) {
        val busy: Boolean get() = saving || signingOut
    }
}
