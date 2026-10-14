package com.orbit.app.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.initialsOf
import com.orbit.app.domain.model.validatePasswordChange
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.feature.auth.executeAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The signed-in user's account: rename, change the password, delete the account, sign out. Each one
 * that ends signed out (delete, sign out) moves the auth gate, so it needs no state here.
 */
class AccountViewModel(private val auth: AuthRepository) : ViewModel() {
    /** The dialogs. Its user fields stay empty: [uiState] fills them from the session. */
    private val form = MutableStateFlow(AccountUiState())

    val uiState: StateFlow<AccountUiState> = combine(auth.session, form) { user, form ->
        form.copy(
            name = user?.displayName.orEmpty(),
            email = user?.email.orEmpty(),
            initials = initialsOf(user?.displayName.orEmpty()),
            photoUrl = user?.photoUrl,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun openEditName() = form.update {
        AccountUiState(dialog = AccountDialog.EDIT_NAME, nameDraft = uiState.value.name)
    }

    fun onNameChange(name: String) = form.update { it.copy(nameDraft = name, authError = null) }

    fun saveName() {
        val state = uiState.value
        if (state.canSaveName) submit("Renaming the user") { auth.updateDisplayName(state.nameDraft.trim()) }
    }

    fun openChangePassword() = form.update { AccountUiState(dialog = AccountDialog.CHANGE_PASSWORD) }

    fun onCurrentPasswordChange(password: String) =
        form.update { it.copy(currentPassword = password, passwordErrors = emptyList(), authError = null) }

    fun onNewPasswordChange(password: String) =
        form.update { it.copy(newPassword = password, passwordErrors = emptyList(), authError = null) }

    fun onConfirmationChange(confirmation: String) =
        form.update { it.copy(confirmation = confirmation, passwordErrors = emptyList(), authError = null) }

    fun togglePasswordsVisibility() = form.update { it.copy(passwordsVisible = !it.passwordsVisible) }

    fun changePassword() {
        val state = form.value
        if (state.submitting) return
        val errors = validatePasswordChange(state.currentPassword, state.newPassword, state.confirmation)
        if (errors.isNotEmpty()) {
            form.update { it.copy(passwordErrors = errors, authError = null) }
            return
        }
        submit("Changing the password") { auth.changePassword(state.currentPassword, state.newPassword) }
    }

    fun openDeleteAccount() = form.update { AccountUiState(dialog = AccountDialog.DELETE_ACCOUNT) }

    fun deleteAccount() = submit("Deleting the account") { auth.deleteAccount() }

    /** Closes the open dialog and forgets what was typed in it, unless its write is still running. */
    fun dismissDialog() = form.update { if (it.submitting) it else AccountUiState() }

    fun signOut() {
        if (form.value.submitting) return
        form.update { it.copy(submitting = true, signOutFailed = false) }
        viewModelScope.launch {
            val error = executeAuth("Signing out") { auth.signOut() }
            form.update { it.copy(submitting = false, signOutFailed = error != null) }
        }
    }

    /** Runs a dialog's [write]: success closes the dialog, a failure stays in it as [AccountUiState.authError]. */
    private fun submit(action: String, write: suspend () -> Unit) {
        if (form.value.submitting) return
        form.update { it.copy(submitting = true, authError = null) }
        viewModelScope.launch {
            val error = executeAuth(action, write)
            form.update { if (error == null) AccountUiState() else it.copy(submitting = false, authError = error) }
        }
    }
}
