package com.orbit.app.feature.auth

/** Where the app starts: the sign-in, or for a signed-in user the list of spaces or straight into one. */
sealed interface SessionStart {
    data object SignedOut : SessionStart

    data object Spaces : SessionStart

    data class Space(val spaceId: String) : SessionStart
}
