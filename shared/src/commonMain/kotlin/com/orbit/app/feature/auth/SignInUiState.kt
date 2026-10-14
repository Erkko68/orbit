package com.orbit.app.feature.auth

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.CredentialsError

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val submitting: Boolean = false,
    /** What the form got wrong, found when Sign in was pressed. Cleared by the next edit. */
    val errors: List<CredentialsError> = emptyList(),
    /** Why the last attempt failed. Cleared by the next edit or attempt. */
    val authError: AuthError? = null,
)
