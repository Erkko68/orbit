package com.orbit.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.validateRegistration
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the create-account form. Registering signs the new user in, which moves the auth gate, so
 * success needs no state here. The mockup asks for no name, so the profile starts with the part of the
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
        _uiState.update { it.copy(submitting = true, errors = emptyList(), authError = null) }
        viewModelScope.launch {
            val email = form.email.trim()
            val authError = try {
                auth.register(email, form.password, displayName = email.substringBefore('@'))
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Registration failed" }
                (e as? AuthException)?.error ?: AuthError.UNKNOWN
            }
            _uiState.update { it.copy(submitting = false, authError = authError) }
        }
    }

    fun signInWithGoogle() {
        val form = _uiState.value
        if (form.submitting) return
        _uiState.update { it.copy(submitting = true, errors = emptyList(), authError = null) }
        viewModelScope.launch {
            val authError = try {
                auth.signInWithGoogle()
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Google sign-in failed" }
                (e as? AuthException)?.error ?: AuthError.UNKNOWN
            }
            _uiState.update { it.copy(submitting = false, authError = authError) }
        }
    }
}
