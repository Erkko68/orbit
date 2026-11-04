package com.orbit.app.feature.member

import com.orbit.app.domain.model.Role
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.members_admins
import orbit.shared.generated.resources.members_members
import orbit.shared.generated.resources.role_owner
import org.jetbrains.compose.resources.StringResource

/** The members of a space, and the role change or handover the owner is making. */
data class SpaceMembersUiState(
    /** The space's name, for the member screen's header and the handover dialog. */
    val spaceName: String = "",
    /** The members have not arrived yet. */
    val loading: Boolean = false,
    /** Loading the members failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    val members: List<SpaceMember> = emptyList(),
    /** The member whose role is changing, or failed to change last. */
    val changing: String? = null,
    /** A role change or a transfer is under way. */
    val busy: Boolean = false,
    /** The last of those failed. Cleared by the next attempt. */
    val failed: Boolean = false,
    /** The member whose role the sheet is picking, while it is open. */
    val pickingRole: SpaceMember? = null,
    /** The admin the user is about to hand the space to, while the dialog asks. */
    val transferTo: SpaceMember? = null,
    /** The user ticked that only the new owner can give the space back. */
    val transferAcknowledged: Boolean = false,
) {
    val canTransfer: Boolean get() = transferTo != null && transferAcknowledged && !busy

    /** Changing the role of [memberId] failed, which their screen says. */
    fun roleFailed(memberId: String): Boolean = failed && changing == memberId

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
    /** The user owns the space and this is someone else: they make a member admin, or an admin member. */
    val canChangeRole: Boolean = false,
    /** The user owns the space and this is an admin, to whom they can hand it. */
    val canTakeOwnership: Boolean = false,
)
