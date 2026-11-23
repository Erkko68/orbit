package com.orbit.app.domain.model

/** [accent] is an index into `OrbitTheme.colors.spaceAccents`, not a colour value. */
data class Space(
    val id: String,
    val name: String,
    val accent: Int,
    val memberIds: List<String>,
    /** The names of those who left, by uid. Their member documents are gone, but their balances are not (#56). */
    val formerMembers: Map<String, String> = emptyMap(),
)
