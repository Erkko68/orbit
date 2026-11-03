package com.orbit.app.feature.space

data class SpaceUiState(
    /** The spaces have not arrived yet. */
    val loading: Boolean = false,
    /** Loading the spaces failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    /** The signed-in user's initials, for the account avatar. */
    val userInitials: String = "",
    /** The signed-in user's photo, for the account avatar. */
    val userPhotoUrl: String? = null,
    /** The signed-in user's spaces. Each one is drawn by a space card, which loads it by id. */
    val spaceIds: List<String> = emptyList(),
    /** An invitation link was opened and its code waits for the new space screen, which offers joining it. */
    val invitationWaiting: Boolean = false,
)
