package com.orbit.app.feature.spacehome.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.initialsOf
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.feature.item.label
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The members of a space, and for the owner the way to change their roles: a member becomes an admin, an admin goes back
 * to member or takes the space over, after a dialog.
 */
class SpaceMembersViewModel(
    private val spaceId: String,
    private val spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val retries = Channel<Unit>(Channel.CONFLATED)

    /** The last role change: whose it is, and whether it is under way or failed, with the handover dialog. */
    private val change = MutableStateFlow(RoleChange())

    val uiState: StateFlow<SpaceMembersUiState> = combine(
        spaces.observeMembers(spaceId).asLoad("Loading the members of $spaceId", retries),
        auth.session,
        change,
    ) { load, user, change ->
        val members = load.valueOrNull.orEmpty()
        val owns = members.firstOrNull { it.userId == user?.id }?.role == Role.OWNER
        val shown = members.mapIndexed { index, member ->
            val other = member.userId != user?.id
            toSpaceMember(
                member,
                index,
                isYou = !other,
                canChangeRole = owns && other && member.role != Role.OWNER,
                canTakeOwnership = owns && other && member.role == Role.ADMIN,
            )
        }
        SpaceMembersUiState(
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            members = shown,
            changing = change.memberId,
            busy = change.busy,
            failed = change.failed,
            transferTo = shown.firstOrNull { it.id == change.memberId && it.canTakeOwnership }?.takeIf { change.confirmingTransfer },
            transferAcknowledged = change.acknowledged,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceMembersUiState(loading = true))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    /** Gives [memberId] [role], when the user may and it is not the one they have. A failure says so on their page. */
    fun changeRole(memberId: String, role: Role) {
        val member = uiState.value.members.firstOrNull { it.id == memberId && it.canChangeRole } ?: return
        if (role == member.currentRole || role == Role.OWNER) return
        submit(memberId, "Changing the role of $memberId") { spaces.changeRole(spaceId, memberId, role) }
    }

    /** Asks before handing the space to [memberId], an admin: the user cannot take it back. */
    fun askTransfer(memberId: String) {
        if (change.value.busy || uiState.value.members.none { it.id == memberId && it.canTakeOwnership }) return
        change.value = RoleChange(memberId, confirmingTransfer = true)
    }

    fun acknowledgeTransfer(acknowledged: Boolean) = change.update { it.copy(acknowledged = acknowledged, failed = false) }

    fun cancelTransfer() {
        if (!change.value.busy) change.value = RoleChange()
    }

    /** Once acknowledged, makes the member in the dialog the owner and the user an admin. */
    fun transferOwnership() {
        if (!uiState.value.canTransfer) return
        val member = uiState.value.transferTo ?: return
        submit(member.id, "Handing space $spaceId to ${member.id}") { spaces.transferOwnership(spaceId, member.id) }
    }

    private fun submit(memberId: String, what: String, write: suspend () -> Unit) {
        if (change.value.busy) return
        change.update { it.copy(memberId = memberId, busy = true, failed = false) }
        viewModelScope.launch {
            val result = attempt(what) { write() }
            change.value = if (result.isSuccess) RoleChange() else change.value.copy(busy = false, failed = true)
        }
    }

    private data class RoleChange(
        val memberId: String? = null,
        val busy: Boolean = false,
        val failed: Boolean = false,
        val confirmingTransfer: Boolean = false,
        val acknowledged: Boolean = false,
    )
}

private fun toSpaceMember(member: Member, index: Int, isYou: Boolean, canChangeRole: Boolean, canTakeOwnership: Boolean) =
    SpaceMember(
        id = member.userId,
        index = index,
        name = member.displayName,
        initials = initialsOf(member.displayName),
        photoUrl = member.photoUrl,
        role = member.role.label,
        currentRole = member.role,
        isYou = isYou,
        canChangeRole = canChangeRole,
        canTakeOwnership = canTakeOwnership,
    )
