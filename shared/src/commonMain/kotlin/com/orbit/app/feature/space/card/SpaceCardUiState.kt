package com.orbit.app.feature.space.card

data class SpaceCardUiState(
    val name: String = "",
    val accent: Int = 0,
    val memberCount: Int = 0,
    /** One entry per member, for the row of avatars. */
    val memberInitials: List<String> = emptyList(),
)
