package com.orbit.app.feature.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The members of a space, and for the owner the way to change their roles, in a sheet: a member becomes an admin, an
 * admin goes back to member or takes the space over, after a dialog.
 */
class SpaceMembersViewModel(
    private val spaceId: String,
    private val spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val retries = Channel<Unit>(Channel.CONFLATED)

    /** The last role change: whose it is, and whether it is under way or failed, with the handover dialog. */
    private val change = MutableStateFlow(RoleChange())

    /** Only decorates the header and the dialog, so it does not hold the members back. */
    private val spaceName = spaces.observeSpaces()
        .orOnFailure("Loading the spaces", emptyList())
        .map { all -> all.firstOrNull { it.id == spaceId }?.name.orEmpty() }
        .onStart { emit("") }

    val uiState: StateFlow<SpaceMembersUiState> = combine(
        spaces.observeMembers(spaceId).asLoad("Loading the members of $spaceId", retries),
        auth.session,
        spaceName,
        change,
    ) { load, user, name, change ->
        val members = load.valueOrNull.orEmpty()
        val mine = members.firstOrNull { it.userId == user?.id }?.role
        val owns = mine == Role.OWNER
        val shown = members.mapIndexed { index, member ->
            val other = member.userId != user?.id
            toSpaceMember(
                member,
                index,
                isYou = !other,
                canChangeRole = owns && other && member.role != Role.OWNER,
                canTakeOwnership = owns && other && member.role == Role.ADMIN,
                canRemove = other && when (mine) {
                    Role.OWNER -> member.role != Role.OWNER
                    Role.ADMIN -> member.role == Role.MEMBER
                    else -> false
                },
            )
        }
        SpaceMembersUiState(
            spaceName = name,
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            members = shown,
            changing = change.memberId,
            busy = change.busy,
            failed = change.failed,
            pickingRole = shown.firstOrNull { it.id == change.memberId && it.canChangeRole }?.takeIf { change.picking },
            removing = shown.firstOrNull { it.id == change.memberId && it.canRemove }?.takeIf { change.confirmingRemoval },
            transferTo = shown.firstOrNull { it.id == change.memberId && it.canTakeOwnership }?.takeIf { change.confirmingTransfer },
            transferAcknowledged = change.acknowledged,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceMembersUiState(loading = true))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    /** Opens the sheet that picks the role of [memberId], when the user may change it. */
    fun openRoles(memberId: String) {
        if (change.value.busy || uiState.value.members.none { it.id == memberId && it.canChangeRole }) return
        change.value = RoleChange(memberId, picking = true)
    }

    fun dismissRoles() {
        if (!change.value.busy) change.value = RoleChange()
    }

    /** Gives [memberId] [role], when the user may. The sheet closes once it is saved, and says so if that fails. */
    fun changeRole(memberId: String, role: Role) {
        val member = uiState.value.members.firstOrNull { it.id == memberId && it.canChangeRole } ?: return
        if (role == Role.OWNER) return
        if (role == member.currentRole) {
            dismissRoles()
            return
        }
        submit(memberId, "Changing the role of $memberId") { spaces.changeRole(spaceId, memberId, role) }
    }

    /** Asks before taking [memberId] out of the space, when the user may: the question names them. */
    fun askRemove(memberId: String) {
        if (change.value.busy || uiState.value.members.none { it.id == memberId && it.canRemove }) return
        change.value = RoleChange(memberId, confirmingRemoval = true)
    }

    fun cancelRemove() {
        if (!change.value.busy) change.value = RoleChange()
    }

    /** Takes the member the dialog asked about out of the space. The dialog stays open if it fails. */
    fun removeMember() {
        val member = uiState.value.removing ?: return
        submit(member.id, "Removing ${member.id} from space $spaceId") {
            spaces.removeMember(spaceId, member.id, member.name)
        }
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
        val picking: Boolean = false,
        val confirmingRemoval: Boolean = false,
        val confirmingTransfer: Boolean = false,
        val acknowledged: Boolean = false,
    )
}

private fun toSpaceMember(
    member: Member,
    index: Int,
    isYou: Boolean,
    canChangeRole: Boolean,
    canTakeOwnership: Boolean,
    canRemove: Boolean,
) =
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
        canRemove = canRemove,
    )
