package com.orbit.app.testing

import com.orbit.app.data.repository.requireUser
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

/** Spaces and members in memory. A new space's only member is its owner. */
class InMemorySpaceRepository(private val auth: AuthRepository) : SpaceRepository {
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

    /** Stands in for joining, which comes with #25, so tests can have more than the owner in a space. */
    fun addMember(spaceId: String, member: Member) {
        spaces.update { all ->
            all.map { if (it.id == spaceId) it.copy(memberIds = it.memberIds + member.userId) else it }
        }
        members.update { all -> all + (spaceId to all[spaceId].orEmpty() + member) }
    }
}
