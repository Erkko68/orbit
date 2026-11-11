package com.orbit.app.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.model.validateNewPassword
import com.orbit.app.domain.model.validatePasswordChange
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.domain.usecase.closeAccount
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
class AccountViewModel(
    private val auth: AuthRepository,
    private val spaces: SpaceRepository,
) : ViewModel() {
    /** The dialogs. Its user fields stay empty: [uiState] fills them from the session. */
    private val form = MutableStateFlow(AccountUiState())

    val uiState: StateFlow<AccountUiState> = combine(auth.session, form) { user, form ->
        form.copy(
            name = user?.displayName.orEmpty(),
            email = user?.email.orEmpty(),
            photoUrl = user?.photoUrl,
            hasPassword = user?.hasPassword != false,
            signsInWithGoogle = user?.signsInWithGoogle != false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUiState())

    fun openEditName() = form.update {
        AccountUiState(dialog = AccountSheetKind.EDIT_NAME, nameDraft = uiState.value.name)
    }

    fun onNameChange(name: String) = form.update { it.copy(nameDraft = name, authError = null) }

    fun saveName() {
        val state = uiState.value
        if (state.canSaveName) submit("Renaming the user") { auth.updateDisplayName(state.nameDraft.trim()) }
    }

    fun openChangePassword() = form.update { AccountUiState(dialog = AccountSheetKind.CHANGE_PASSWORD) }

    fun onCurrentPasswordChange(password: String) =
        form.update { it.copy(currentPassword = password, passwordErrors = emptyList(), authError = null) }

    fun onNewPasswordChange(password: String) =
        form.update { it.copy(newPassword = password, passwordErrors = emptyList(), authError = null) }

    fun onConfirmationChange(confirmation: String) =
        form.update { it.copy(confirmation = confirmation, passwordErrors = emptyList(), authError = null) }

    fun togglePasswordsVisibility() = form.update { it.copy(passwordsVisible = !it.passwordsVisible) }

    /** Changes the password, or sets the first one of an account that only signs in with Google (#293). */
    fun changePassword() {
        val state = form.value
        if (state.submitting) return
        val setting = !uiState.value.hasPassword
        val errors = if (setting) {
            validateNewPassword(state.newPassword, state.confirmation)
        } else {
            validatePasswordChange(state.currentPassword, state.newPassword, state.confirmation)
        }
        if (errors.isNotEmpty()) {
            form.update { it.copy(passwordErrors = errors, authError = null) }
            return
        }
        if (setting) {
            submit("Setting a password") { auth.setPassword(state.newPassword) }
        } else {
            submit("Changing the password") { auth.changePassword(state.currentPassword, state.newPassword) }
        }
    }

    fun openConnectGoogle() = form.update { AccountUiState(dialog = AccountSheetKind.CONNECT_GOOGLE) }

    /** Opens Google's account picker; the one picked becomes another way into this account (#293). */
    fun connectGoogle() = submit("Connecting Google") { auth.linkGoogle() }

    fun openDeleteAccount() = form.update { AccountUiState(dialog = AccountSheetKind.DELETE_ACCOUNT) }

    fun onDeletePasswordChange(password: String) =
        form.update { it.copy(deletePassword = password, authError = null, handOver = emptyList()) }

    /**
     * Deletes the account, or answers with the spaces to hand over first. Unlike the other dialogs this one
     * has two ways of not going through: a refusal the user can act on, which stays in the sheet as a list
     * of spaces, and an ordinary failure, which stays as [AccountUiState.authError].
     */
    fun deleteAccount() {
        val state = form.value
        if (state.submitting) return
        form.update { it.copy(submitting = true, authError = null, handOver = emptyList()) }
        viewModelScope.launch {
            var handOver = emptyList<String>()
            val error = executeAuth("Deleting the account") { handOver = closeAccount(auth, spaces, state.deletePassword) }
            form.update {
                when {
                    error != null -> it.copy(submitting = false, authError = error)
                    handOver.isNotEmpty() -> it.copy(submitting = false, handOver = handOver)
                    else -> AccountUiState()
                }
            }
        }
    }

    /** Closes the open dialog and forgets what was typed in it, unless its write is still running. */
    fun dismissDialog() = form.update { if (it.submitting) it else AccountUiState() }

    fun openSignOut() = form.update { AccountUiState(dialog = AccountSheetKind.SIGN_OUT) }

    fun signOut() = submit("Signing out") { auth.signOut() }

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
