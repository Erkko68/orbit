package com.orbit.app.feature.auth

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.CredentialsError

data class CreateAccountUiState(
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val passwordVisible: Boolean = false,
    val confirmationVisible: Boolean = false,
    val submitting: Boolean = false,
    /** What the form got wrong, found when Create account was pressed. Cleared by the next edit. */
    val errors: List<CredentialsError> = emptyList(),
    /** Why the last attempt failed. Cleared by the next edit or attempt. */
    val authError: AuthError? = null,
)
