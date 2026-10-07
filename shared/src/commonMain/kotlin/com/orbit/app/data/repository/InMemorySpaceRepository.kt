package com.orbit.app.data.repository

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

/** Stand-in until the Firestore repository replaces it (#21). Data lasts while the app runs. */
class InMemorySpaceRepository(private val auth: AuthRepository) : SpaceRepository {
    private val spaces = MutableStateFlow(emptyList<Space>())
    private val members = MutableStateFlow(emptyList<Member>())

    override fun observeSpaces(): Flow<List<Space>> = combine(auth.session, spaces) { user, all ->
        all.filter { user != null && user.id in it.memberIds }
    }

    override fun observeMembers(spaceId: String): Flow<List<Member>> =
        members.map { all -> all.filter { it.spaceId == spaceId } }

    override suspend fun createSpace(name: String, accent: Int): String {
        val user = auth.requireUser()
        val before = spaces.getAndUpdate { it + Space("space-${it.size + 1}", name, accent, listOf(user.id)) }
        val id = "space-${before.size + 1}"
        members.update { it + Member(user.id, id, user.displayName, user.photoUrl, Role.OWNER) }
        return id
    }
}
