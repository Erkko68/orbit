package com.orbit.app.domain.model

/** The longest display name, so a name never breaks a layout that shows it (#293). `firestore.rules` checks the same. */
const val DISPLAY_NAME_MAX_LENGTH = 30

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
    /**
     * The account signs in with a password, not only with Google. An account without one has no password
     * to change, and nothing to re-enter before it is deleted (#188).
     */
    val hasPassword: Boolean = false,
    /** Google is one of the account's ways in. Without it, the account screen offers to connect it (#293). */
    val signsInWithGoogle: Boolean = false,
)
