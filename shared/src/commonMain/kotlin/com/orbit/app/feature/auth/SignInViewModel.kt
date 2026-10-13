package com.orbit.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the sign-in form. Placeholder until the sign-in form (#16): no validation yet, and a blank
 * email signs in a fixed account. Signing in moves the auth gate, so success needs no state here.
 */
class SignInViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, failed = false) }

    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, failed = false) }

    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun signIn() {
        val form = _uiState.value
        if (form.submitting) return
        _uiState.update { it.copy(submitting = true, failed = false) }
        viewModelScope.launch {
            val failed = try {
                auth.signIn(form.email.trim().ifEmpty { PLACEHOLDER_EMAIL }, form.password)
                false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Sign-in failed" }
                true
            }
            _uiState.update { it.copy(submitting = false, failed = failed) }
        }
    }

    private companion object {
        const val PLACEHOLDER_EMAIL = "you@orbit.app"
    }
}
