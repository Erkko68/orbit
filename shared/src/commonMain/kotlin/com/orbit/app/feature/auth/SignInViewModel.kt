package com.orbit.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.model.validateEmail
import com.orbit.app.domain.model.validateSignIn
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds the sign-in form. Signing in moves the auth gate, so success needs no state here. */
class SignInViewModel(private val auth: AuthRepository) : ViewModel(), AuthLinkForm {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    /** Submissions and the link sheet, both shared with the other auth form (#173). */
    private val form = AuthForm(auth, _uiState, viewModelScope)

    override val linking get() = form.linking

    fun onEmailChange(email: String) =
        _uiState.update { it.copy(email = email, errors = emptyList(), authError = null) }

    fun onPasswordChange(password: String) =
        _uiState.update { it.copy(password = password, errors = emptyList(), authError = null) }

    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun signIn() {
        val form = _uiState.value
        if (form.submitting) return
        val errors = validateSignIn(form.email, form.password)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, authError = null) }
            return
        }
        submit("Sign-in") { auth.signIn(form.email.trim(), form.password) }
    }

    override fun signInWithGoogle() = submit("Google sign-in") { auth.signInWithGoogle() }

    /** Opens the reset dialog on whatever address the form already has (#217). */
    fun openPasswordReset() {
        if (_uiState.value.submitting) return
        _uiState.update {
            it.copy(
                resetOpen = true,
                resetEmail = it.email,
                resetErrors = emptyList(),
                resetSent = false,
                resetError = null,
            )
        }
    }

    fun onResetEmailChange(email: String) =
        _uiState.update { it.copy(resetEmail = email, resetErrors = emptyList(), resetError = null) }

    /** Closes the dialog, unless the link is already going out. */
    fun dismissPasswordReset() {
        if (!_uiState.value.resetSending) _uiState.update { it.copy(resetOpen = false) }
    }

    /**
     * Sends the link. The dialog then says it went, whether or not the address has an account, so nobody
     * learns which addresses are registered; only a failure of the request itself is shown.
     */
    fun sendPasswordReset() {
        val form = _uiState.value
        if (form.resetSending) return
        val errors = validateEmail(form.resetEmail)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(resetErrors = errors, resetError = null) }
            return
        }
        _uiState.update { it.copy(resetSending = true, resetErrors = emptyList(), resetError = null) }
        viewModelScope.launch {
            val authError = executeAuth("Password reset") { auth.sendPasswordReset(form.resetEmail.trim()) }
            _uiState.update { it.copy(resetSending = false, resetError = authError, resetSent = authError == null) }
        }
    }

    /** Runs [call] unless one is running; the form then shows whatever it left behind. */
    private fun submit(what: String, call: suspend () -> Unit) = form.submit(what, call)
}
