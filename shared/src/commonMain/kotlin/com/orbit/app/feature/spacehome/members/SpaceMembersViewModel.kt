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

/** The members of a space, and for an owner or admin the way to change their roles or hand the space over. */
class SpaceMembersViewModel(
    private val spaceId: String,
    private val spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val retries = Channel<Unit>(Channel.CONFLATED)

    /** The role picker: which member it is for, and whether its write is under way or failed. */
    private val picker = MutableStateFlow(Picker())

    val uiState: StateFlow<SpaceMembersUiState> = combine(
        spaces.observeMembers(spaceId).asLoad("Loading the members of $spaceId", retries),
        auth.session,
        picker,
    ) { load, user, picker ->
        val members = load.valueOrNull.orEmpty()
        val myRole = members.firstOrNull { it.userId == user?.id }?.role
        val shown = members.map { member ->
            val other = member.userId != user?.id
            toSpaceMember(
                member,
                isYou = !other,
                canChangeRole = myRole?.managesSpace == true && member.role != Role.OWNER && other,
                canTakeOwnership = myRole == Role.OWNER && other,
            )
        }
        SpaceMembersUiState(
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            members = shown,
            rolePicker = shown.firstOrNull { it.id == picker.memberId && (it.canChangeRole || it.canTakeOwnership) },
            busy = picker.busy,
            failed = picker.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceMembersUiState(loading = true))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    /** Opens the picker for a member whose role the user can change or who can take the space over. */
    fun openRolePicker(memberId: String) {
        picker.value = Picker(memberId)
    }

    fun closeRolePicker() {
        picker.value = Picker()
    }

    /** Gives the member in the picker [role] and closes it. It stays open, saying so, if that fails. */
    fun changeRole(role: Role) {
        val member = uiState.value.rolePicker?.takeIf { it.canChangeRole } ?: return
        submit("Changing the role of ${member.id}") { spaces.changeRole(spaceId, member.id, role) }
    }

    /** Makes the member in the picker the owner, and the user an admin. */
    fun transferOwnership() {
        val member = uiState.value.rolePicker?.takeIf { it.canTakeOwnership } ?: return
        submit("Handing space $spaceId to ${member.id}") { spaces.transferOwnership(spaceId, member.id) }
    }

    private fun submit(what: String, write: suspend () -> Unit) {
        if (picker.value.busy) return
        picker.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            val result = attempt(what) { write() }
            picker.value = if (result.isSuccess) Picker() else picker.value.copy(busy = false, failed = true)
        }
    }

    private data class Picker(val memberId: String? = null, val busy: Boolean = false, val failed: Boolean = false)
}

private fun toSpaceMember(member: Member, isYou: Boolean, canChangeRole: Boolean, canTakeOwnership: Boolean) =
    SpaceMember(
        id = member.userId,
        name = member.displayName,
        initials = initialsOf(member.displayName),
        photoUrl = member.photoUrl,
        role = member.role.label,
        currentRole = member.role,
        isYou = isYou,
        canChangeRole = canChangeRole,
        canTakeOwnership = canTakeOwnership,
    )
