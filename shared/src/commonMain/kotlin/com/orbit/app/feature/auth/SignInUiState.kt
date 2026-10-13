package com.orbit.app.feature.auth

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val submitting: Boolean = false,
    /** The last attempt failed. Cleared by the next edit or attempt. */
    val failed: Boolean = false,
)
