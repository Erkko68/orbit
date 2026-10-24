package com.orbit.app.testing

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * A space with members in any role, which the in-memory repository cannot build (it only adds owners).
 * A role change or a transfer updates the members as the real one would; with [writeFailure] every write throws
 * that instead. It has no signed-in user, so the transfer makes the current owner an admin.
 */
internal class FakeSpaceRepository(
    private val space: Space,
    members: List<Member>,
    private val writeFailure: Exception? = null,
) : SpaceRepository {
    private val members = MutableStateFlow(members)

    override fun observeSpaces(): Flow<List<Space>> = flowOf(listOf(space))

    override fun observeMembers(spaceId: String): Flow<List<Member>> =
        members.map { if (spaceId == space.id) it else emptyList() }

    override suspend fun createSpace(name: String, accent: Int): String = error("Not used by these tests")

    override suspend fun changeRole(spaceId: String, userId: String, role: Role) {
        writeFailure?.let { throw it }
        members.update { all -> all.map { if (it.userId == userId) it.copy(role = role) else it } }
    }

    override suspend fun joinSpace(code: String): String = error("Not used by these tests")

    override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) = error("Not used by these tests")

    override suspend fun leaveSpace(spaceId: String) {
        writeFailure?.let { throw it }
        error("Has no signed-in user: leave through InMemorySpaceRepository")
    }

    override suspend fun deleteSpace(spaceId: String) {
        writeFailure?.let { throw it }
        error("Has no signed-in user: delete through InMemorySpaceRepository")
    }

    override suspend fun transferOwnership(spaceId: String, userId: String) {
        writeFailure?.let { throw it }
        members.update { all ->
            all.map {
                when {
                    it.userId == userId -> it.copy(role = Role.OWNER)
                    it.role == Role.OWNER -> it.copy(role = Role.ADMIN)
                    else -> it
                }
            }
        }
    }

    companion object {
        fun withMembers(vararg people: Pair<String, Role>, writeFailure: Exception? = null) = FakeSpaceRepository(
            space = Space("space-1", "Flat 4B", accent = 0, memberIds = people.map { it.first }),
            members = people.map { (name, role) -> Member(name, name, photoUrl = null, role = role) },
            writeFailure = writeFailure,
        )
    }
}
