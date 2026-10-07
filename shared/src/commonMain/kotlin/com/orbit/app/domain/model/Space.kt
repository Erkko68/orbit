package com.orbit.app.domain.model

/** [accent] is an index into `OrbitTheme.colors.spaceAccents`, not a colour value. */
data class Space(
    val id: String,
    val name: String,
    val accent: Int,
    val memberIds: List<String>,
)
