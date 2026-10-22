package com.orbit.app.testing

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * One space, Flat 4B, owned by ana. Its spaces fail to load the first [spaceFailures] times they are
 * observed, and its members the first [memberFailures] times.
 */
internal class FlakySpaceRepository(private var spaceFailures: Int = 0, private var memberFailures: Int = 0) :
    SpaceRepository {
    override fun observeSpaces(): Flow<List<Space>> = flow {
        if (spaceFailures > 0) {
            spaceFailures--
            error("Could not reach the server")
        }
        emit(listOf(Space("space-1", "Flat 4B", accent = 0, memberIds = listOf("ana"))))
    }

    override fun observeMembers(spaceId: String): Flow<List<Member>> = flow {
        if (memberFailures > 0) {
            memberFailures--
            error("Could not reach the server")
        }
        emit(listOf(Member("ana", "ana", photoUrl = null, role = Role.OWNER)))
    }

    override suspend fun createSpace(name: String, accent: Int): String = error("Not used by these tests")

    override suspend fun changeRole(spaceId: String, userId: String, role: Role) = error("Not used by these tests")

    override suspend fun joinSpace(code: String): String = error("Not used by these tests")

    override suspend fun updateSpace(spaceId: String, name: String?, accent: Int?) = error("Not used by these tests")
}
