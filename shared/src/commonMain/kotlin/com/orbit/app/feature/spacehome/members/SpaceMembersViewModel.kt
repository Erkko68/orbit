package com.orbit.app.feature.spacehome.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.initialsOf
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.role_admin
import orbit.shared.generated.resources.role_member
import orbit.shared.generated.resources.role_owner

/** The members tab of a space: its members and their roles. */
class SpaceMembersViewModel(spaceId: String, spaces: SpaceRepository) : ViewModel() {
    val uiState: StateFlow<SpaceMembersUiState> = spaces.observeMembers(spaceId)
        .map { members -> SpaceMembersUiState(members.map(::toSpaceMember)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceMembersUiState())
}

private fun toSpaceMember(member: Member) = SpaceMember(
    id = member.userId,
    name = member.displayName,
    initials = initialsOf(member.displayName),
    photoUrl = member.photoUrl,
    role = when (member.role) {
        Role.OWNER -> Res.string.role_owner
        Role.ADMIN -> Res.string.role_admin
        Role.MEMBER -> Res.string.role_member
    },
)
