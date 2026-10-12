package com.orbit.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * Placeholder until the sign-in form: no validation or error messages yet, and a blank email signs
 * in a fixed account. The in-memory AuthRepository accepts any credentials.
 */
class SignInViewModel(private val auth: AuthRepository) : ViewModel() {
    fun signIn(email: String, password: String) {
        viewModelScope.launch { auth.signIn(email.trim().ifEmpty { PLACEHOLDER_EMAIL }, password) }
    }

    private companion object {
        const val PLACEHOLDER_EMAIL = "you@orbit.app"
    }
}
