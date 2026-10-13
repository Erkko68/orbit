package com.orbit.app.testing

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/** Fails to load its spaces the first [failures] times they are observed, then has one space. */
internal class FlakySpaceRepository(private var failures: Int) : SpaceRepository {
    override fun observeSpaces(): Flow<List<Space>> = flow {
        if (failures > 0) {
            failures--
            error("Could not reach the server")
        }
        emit(listOf(Space("space-1", "Flat 4B", accent = 0, memberIds = listOf("ana"))))
    }

    override fun observeMembers(spaceId: String): Flow<List<Member>> =
        flowOf(listOf(Member("ana", spaceId, "ana", photoUrl = null, role = Role.OWNER)))

    override suspend fun createSpace(name: String, accent: Int): String = error("Not used by these tests")
}
