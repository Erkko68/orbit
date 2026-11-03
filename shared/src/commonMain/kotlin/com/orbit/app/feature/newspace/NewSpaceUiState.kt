package com.orbit.app.feature.newspace

import com.orbit.app.domain.model.JoinError

data class NewSpaceUiState(
    val name: String = "",
    /** The colour picked for the space, which the screen takes on as a preview. */
    val accent: Int = 0,
    val creating: Boolean = false,
    /** The last create failed. Cleared by the next edit or attempt. */
    val createFailed: Boolean = false,
    /** What is typed or pasted in the join field: an invitation code or link. */
    val joinCode: String = "",
    val joining: Boolean = false,
    /** Why the last join did not work. Cleared by the next edit or attempt. */
    val joinError: JoinError? = null,
    /** The space the user just created or joined, which the screen opens. */
    val openSpaceId: String? = null,
) {
    val canCreate: Boolean get() = name.isNotBlank() && !creating
    val canJoin: Boolean get() = joinCode.isNotBlank() && !joining
}
