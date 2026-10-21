package com.orbit.app.feature.space

import com.orbit.app.domain.model.JoinError

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
    /** The "Create a new space" dialog is showing. */
    val createOpen: Boolean = false,
    /** What is typed in that dialog. */
    val newSpaceName: String = "",
    val creating: Boolean = false,
    /** The last create failed. Cleared by the next edit or attempt. */
    val createFailed: Boolean = false,
    /** What is typed or pasted in the join field: an invitation code or link. */
    val joinCode: String = "",
    val joining: Boolean = false,
    /** Why the last join did not work. Cleared by the next edit or attempt. */
    val joinError: JoinError? = null,
    /** An invitation link was opened and its code waits for the screen to offer joining it. */
    val invitationWaiting: Boolean = false,
) {
    val canCreate: Boolean get() = newSpaceName.isNotBlank() && !creating
    val canJoin: Boolean get() = joinCode.isNotBlank() && !joining
    val spaceCount: Int get() = spaceIds.size
}
