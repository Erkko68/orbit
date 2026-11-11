package com.orbit.app.feature.space.card

import com.orbit.app.core.initialsOf

data class SpaceCardUiState(
    val name: String = "",
    val accent: Int = 0,
    val memberCount: Int = 0,
    /** One entry per member, for the faces on the card. */
    val faces: List<SpaceCardFace> = emptyList(),
    /** What is on the user's plate today, and what is open in the whole space. Null until the items load, or if they could not. */
    val dueToday: Int? = null,
    val openCount: Int? = null,
) {
    /** What the space's badge says, so it is told apart at a glance (#293). */
    val initials: String get() = initialsOf(name)
}

/** A member on the card: their photo, or their initials until it loads or when there is none. */
data class SpaceCardFace(val id: String, val name: String, val initials: String, val photoUrl: String?)
