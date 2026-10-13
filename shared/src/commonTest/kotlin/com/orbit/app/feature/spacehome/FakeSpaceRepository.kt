package com.orbit.app.feature.spacehome

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** A space with members in any role, which the in-memory repository cannot build (it only adds owners). */
internal class FakeSpaceRepository(private val space: Space, private val members: List<Member>) : SpaceRepository {
    override fun observeSpaces(): Flow<List<Space>> = flowOf(listOf(space))

    override fun observeMembers(spaceId: String): Flow<List<Member>> =
        flowOf(members.filter { it.spaceId == spaceId })

    override suspend fun createSpace(name: String, accent: Int): String = error("Not used by these tests")

    companion object {
        fun withMembers(vararg people: Pair<String, Role>) = FakeSpaceRepository(
            space = Space("space-1", "Flat 4B", accent = 0, memberIds = people.map { it.first }),
            members = people.map { (name, role) -> Member(name, "space-1", name, photoUrl = null, role = role) },
        )
    }
}
