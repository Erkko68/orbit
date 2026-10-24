package com.orbit.app.testing

import com.orbit.app.data.repository.requireUser
import com.orbit.app.domain.model.JoinError
import com.orbit.app.domain.model.JoinException
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/**
 * Spaces and members in memory. A new space only member is its owner. Joining looks the code up in
 * [invitations], so a test that joins gives it the invitations it created.
 */
class InMemorySpaceRepository(
    private val auth: AuthRepository,
    private val invitations: InMemoryInvitationRepository? = null,
    private val clock: Clock = Clock.System,
) : SpaceRepository {
    private val spaces = MutableStateFlow(emptyList<Space>())
    private val members = MutableStateFlow(emptyMap<String, List<Member>>())

    override fun observeSpaces(): Flow<List<Space>> = combine(auth.session, spaces) { user, all ->
        all.filter { user != null && user.id in it.memberIds }
    }

    override fun observeMembers(spaceId: String): Flow<List<Member>> =
        members.map { it[spaceId].orEmpty() }

    override suspend fun createSpace(name: String, accent: Int): String {
        val user = auth.requireUser()
        val before = spaces.getAndUpdate { it + Space("space-${it.size + 1}", name, accent, listOf(user.id)) }
        val id = "space-${before.size + 1}"
        members.update { it + (id to listOf(Member(user.id, user.displayName, user.photoUrl, Role.OWNER))) }
        return id
    }

    override suspend fun changeRole(spaceId: String, userId: String, role: Role) {
        require(role != Role.OWNER) { "Ownership moves only through a transfer" }
        val actorId = auth.requireUser().id
        val team = members.value[spaceId].orEmpty()
        check(team.firstOrNull { it.userId == actorId }?.role?.managesSpace == true) {
            "Only an owner or admin can change a role"
        }
        val target = checkNotNull(team.firstOrNull { it.userId == userId }) { "Not a member of the space" }
        check(target.role != Role.OWNER) { "The role of the owner cannot change" }
        members.update { all ->
            all + (spaceId to team.map { if (it.userId == userId) it.copy(role = role) else it })
        }
    }

    override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) {
        require(name != null || accent != null) { "Nothing to change" }
        require(name == null || name.isNotBlank()) { "A space needs a name" }
        val actorId = auth.requireUser().id
        check(members.value[spaceId].orEmpty().firstOrNull { it.userId == actorId }?.role?.managesSpace == true) {
            "Only an owner or admin can edit a space"
        }
        spaces.update { all ->
            all.map { if (it.id == spaceId) it.copy(name = name ?: it.name, accent = accent ?: it.accent) else it }
        }
    }

    override suspend fun leaveSpace(spaceId: String) {
        val userId = auth.requireUser().id
        val team = members.value[spaceId].orEmpty()
        val me = checkNotNull(team.firstOrNull { it.userId == userId }) { "Not a member of the space" }
        check(me.role != Role.OWNER) { "The owner deletes the space or hands it over" }
        spaces.update { all ->
            all.map { if (it.id == spaceId) it.copy(memberIds = it.memberIds - userId) else it }
        }
        members.update { all -> all + (spaceId to team - me) }
    }

    override suspend fun deleteSpace(spaceId: String) {
        val userId = auth.requireUser().id
        check(members.value[spaceId].orEmpty().firstOrNull { it.userId == userId }?.role == Role.OWNER) {
            "Only the owner can delete the space"
        }
        spaces.update { all -> all.filterNot { it.id == spaceId } }
        members.update { it - spaceId }
    }

    override suspend fun transferOwnership(spaceId: String, userId: String) {
        val actorId = auth.requireUser().id
        val team = members.value[spaceId].orEmpty()
        check(team.firstOrNull { it.userId == actorId }?.role == Role.OWNER) { "Only the owner can hand it over" }
        check(userId != actorId && team.any { it.userId == userId }) { "Not another member of the space" }
        val roles = mapOf(userId to Role.OWNER, actorId to Role.ADMIN)
        members.update { all -> all + (spaceId to team.map { it.copy(role = roles[it.userId] ?: it.role) }) }
    }

    /** Gives a space a member without an invitation, so tests can have more than the owner in it. */
    fun addMember(spaceId: String, member: Member) {
        spaces.update { all ->
            all.map { if (it.id == spaceId) it.copy(memberIds = it.memberIds + member.userId) else it }
        }
        members.update { all -> all + (spaceId to all[spaceId].orEmpty() + member) }
    }

    override suspend fun joinSpace(code: String): String {
        val user = auth.requireUser()
        val invitation = invitations?.find(code) ?: throw JoinException(JoinError.INVALID)
        if (invitation.revoked) throw JoinException(JoinError.REVOKED)
        if (!invitation.isActive(clock.now())) throw JoinException(JoinError.EXPIRED)
        val space = spaces.value.firstOrNull { it.id == invitation.spaceId } ?: throw JoinException(JoinError.INVALID)
        if (user.id in space.memberIds) throw JoinException(JoinError.ALREADY_MEMBER)
        addMember(space.id, Member(user.id, user.displayName, user.photoUrl, Role.MEMBER))
        return space.id
    }
}
