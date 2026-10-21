package com.orbit.app.feature.spacehome.members

import com.orbit.app.domain.model.Role
import org.jetbrains.compose.resources.StringResource

data class SpaceMembersUiState(
    /** The members have not arrived yet. */
    val loading: Boolean = false,
    /** Loading the members failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    val members: List<SpaceMember> = emptyList(),
    /** The member whose role is being picked, while the picker is open. */
    val rolePicker: SpaceMember? = null,
    /** A role is being changed. */
    val busy: Boolean = false,
    /** The last role change failed. Cleared by the next attempt or by closing the picker. */
    val failed: Boolean = false,
)

data class SpaceMember(
    val id: String,
    val name: String,
    val initials: String,
    val photoUrl: String?,
    val role: StringResource,
    val currentRole: Role,
    /** The user can change this role: they manage the space, and this is neither the owner nor themselves. */
    val canChangeRole: Boolean = false,
)
