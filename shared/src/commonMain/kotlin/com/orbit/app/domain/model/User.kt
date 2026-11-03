package com.orbit.app.domain.model

/**
 * The signed-in user's profile. [email] is the sign-in account's, not part of the profile document. The
 * document also keeps the phone's language and time zone, for the server; the app reads them from the phone.
 */
data class User(
    val id: String,
    val displayName: String,
    val photoUrl: String?,
    val email: String? = null,
    /** They accepted what the assistant sends to the AI provider, once for the account. */
    val aiConsent: Boolean = false,
)
