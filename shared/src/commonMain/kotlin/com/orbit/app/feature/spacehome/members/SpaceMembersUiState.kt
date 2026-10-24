package com.orbit.app.feature.spacehome.members

import com.orbit.app.domain.model.Role
import org.jetbrains.compose.resources.StringResource

/** The member row of a space: its chips and the role picker of another member. */
data class SpaceMembersUiState(
    /** The members have not arrived yet. */
    val loading: Boolean = false,
    /** Loading the members failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    val members: List<SpaceMember> = emptyList(),
    /** The member whose role is being picked, while the picker is open. */
    val rolePicker: SpaceMember? = null,
    /** A role change or a transfer is under way. */
    val busy: Boolean = false,
    /** The last of those failed. Cleared by the next attempt or by closing the picker. */
    val failed: Boolean = false,
)

data class SpaceMember(
    val id: String,
    val name: String,
    val initials: String,
    val photoUrl: String?,
    val role: StringResource,
    val currentRole: Role,
    /** The signed-in user: their chip says "You". */
    val isYou: Boolean = false,
    /** The user can change this role: they manage the space, and this is neither the owner nor themselves. */
    val canChangeRole: Boolean = false,
    /** The user owns the space and can hand it to this member. */
    val canTakeOwnership: Boolean = false,
)
