package com.orbit.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.model.validateSignIn
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Holds the sign-in form. Signing in moves the auth gate, so success needs no state here. */
class SignInViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    /** The link sheet, shared with the other auth form: a collision is state here, with its own message (#173). */
    private val linking = AccountLinking(
        auth = auth,
        scope = viewModelScope,
        read = { _uiState.value.link },
        write = { link -> _uiState.update { it.copy(link = link) } },
    )

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

    fun signInWithGoogle() = submit("Google sign-in") { auth.signInWithGoogle() }

    fun onLinkPasswordChange(password: String) = linking.onPasswordChange(password)

    fun toggleLinkPasswordVisibility() = linking.togglePasswordVisibility()

    fun confirmLink() = linking.confirm()

    fun dismissLink() = linking.dismiss()

    /**
     * Runs [call] unless one is running. A failure stays in the form as [SignInUiState.authError]; an
     * address that already has an account becomes [SignInUiState.link], which offers to link it.
     */
    private fun submit(what: String, call: suspend () -> Unit) {
        if (_uiState.value.submitting) return
        _uiState.update { it.copy(submitting = true, errors = emptyList(), authError = null) }
        viewModelScope.launch {
            when (val outcome = executeSignIn(what, call)) {
                null -> _uiState.update { it.copy(submitting = false) }
                is SignInOutcome.Link -> _uiState.update {
                    it.copy(submitting = false, link = AccountLinkUiState(outcome.email, outcome.proof))
                }
                is SignInOutcome.Failed -> _uiState.update { it.copy(submitting = false, authError = outcome.error) }
            }
        }
    }
}
