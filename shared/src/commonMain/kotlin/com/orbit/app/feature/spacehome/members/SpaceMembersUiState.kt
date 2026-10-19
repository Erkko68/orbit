package com.orbit.app.feature.spacehome.members

import org.jetbrains.compose.resources.StringResource

data class SpaceMembersUiState(
    /** The members have not arrived yet. */
    val loading: Boolean = false,
    /** Loading the members failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    val members: List<SpaceMember> = emptyList(),
)

data class SpaceMember(
    val id: String,
    val name: String,
    val initials: String,
    val photoUrl: String?,
    val role: StringResource,
)
