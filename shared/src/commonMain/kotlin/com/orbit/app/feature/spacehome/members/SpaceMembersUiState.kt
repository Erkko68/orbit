package com.orbit.app.feature.spacehome.members

import com.orbit.app.domain.model.Role
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.members_admins
import orbit.shared.generated.resources.members_members
import orbit.shared.generated.resources.role_owner
import org.jetbrains.compose.resources.StringResource

/** The members of a space, and the role picker of another member. */
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
    /** The dialog that asks before handing the space over is open. */
    val confirmingTransfer: Boolean = false,
    /** The user ticked that only the new owner can give the space back. */
    val transferAcknowledged: Boolean = false,
) {
    val canTransfer: Boolean get() = confirmingTransfer && transferAcknowledged && !busy

    /** The members under their role, the owner first. Within a role the user comes first, then by name. */
    val byRole: List<RoleSection>
        get() = Role.entries.mapNotNull { role ->
            members.filter { it.currentRole == role }
                .sortedWith(compareByDescending<SpaceMember> { it.isYou }.thenBy { it.name.lowercase() })
                .takeIf { it.isNotEmpty() }
                ?.let { RoleSection(role.sectionTitle, it) }
        }
}

/** The members who have one role, under its [title]. */
data class RoleSection(val title: StringResource, val members: List<SpaceMember>)

private val Role.sectionTitle: StringResource
    get() = when (this) {
        Role.OWNER -> Res.string.role_owner
        Role.ADMIN -> Res.string.members_admins
        Role.MEMBER -> Res.string.members_members
    }

data class SpaceMember(
    val id: String,
    /** Their place in the space, which keeps their colour and shape wherever they are listed. */
    val index: Int,
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
