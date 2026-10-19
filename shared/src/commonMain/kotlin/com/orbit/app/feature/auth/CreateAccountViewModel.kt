package com.orbit.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.model.validateRegistration
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the create-account form. Registering signs the new user in, which moves the auth gate, so
 * success needs no state here. The form asks for no name, so the profile starts with the part of the
 * email before the `@`, which the account screen can edit later.
 */
class CreateAccountViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateAccountUiState())
    val uiState: StateFlow<CreateAccountUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) =
        _uiState.update { it.copy(email = email, errors = emptyList(), authError = null) }

    fun onPasswordChange(password: String) =
        _uiState.update { it.copy(password = password, errors = emptyList(), authError = null) }

    fun onConfirmationChange(confirmation: String) =
        _uiState.update { it.copy(confirmation = confirmation, errors = emptyList(), authError = null) }

    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun toggleConfirmationVisibility() = _uiState.update { it.copy(confirmationVisible = !it.confirmationVisible) }

    fun createAccount() {
        val form = _uiState.value
        if (form.submitting) return
        val errors = validateRegistration(form.email, form.password, form.confirmation)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, authError = null) }
            return
        }
        val email = form.email.trim()
        submit("Registration") { auth.register(email, form.password, displayName = email.substringBefore('@')) }
    }

    fun signInWithGoogle() = submit("Google sign-in") { auth.signInWithGoogle() }

    /** Runs [call] unless one is running. A failure stays in the form as [CreateAccountUiState.authError]. */
    private fun submit(what: String, call: suspend () -> Unit) {
        if (_uiState.value.submitting) return
        _uiState.update { it.copy(submitting = true, errors = emptyList(), authError = null) }
        viewModelScope.launch {
            val authError = executeAuth(what, call)
            _uiState.update { it.copy(submitting = false, authError = authError) }
        }
    }
}
