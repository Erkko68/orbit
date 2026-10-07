package com.orbit.app.domain.repository

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Space
import kotlinx.coroutines.flow.Flow

/**
 * Acts as the signed-in user: callers never pass a user id. Writes suspend until done and throw
 * on failure.
 */
interface SpaceRepository {
    /** The spaces the signed-in user is a member of. Empty when signed out. */
    fun observeSpaces(): Flow<List<Space>>

    fun observeMembers(spaceId: String): Flow<List<Member>>

    /** Creates a space owned by the signed-in user and returns its id. */
    suspend fun createSpace(name: String, accent: Int): String
}
