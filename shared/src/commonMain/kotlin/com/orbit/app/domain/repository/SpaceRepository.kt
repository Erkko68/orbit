package com.orbit.app.domain.repository

import com.orbit.app.domain.model.JoinException
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import kotlinx.coroutines.flow.Flow

/**
 * Acts as the signed-in user: callers never pass a user id. Writes return once applied locally,
 * without waiting for the server, so they work offline. They throw on failure.
 */
interface SpaceRepository {
    /** The spaces the signed-in user is a member of. Empty when signed out. */
    fun observeSpaces(): Flow<List<Space>>

    fun observeMembers(spaceId: String): Flow<List<Member>>

    /** Creates a space owned by the signed-in user and returns its id. */
    suspend fun createSpace(name: String, accent: Int): String

    /**
     * Sets the role of another member to [Role.ADMIN] or [Role.MEMBER]. Only an owner or admin can, the
     * role of the owner never changes, and nobody becomes owner this way: ownership moves only through a
     * transfer (#27). Throws when it is not allowed.
     */
    suspend fun changeRole(spaceId: String, userId: String, role: Role)

    /**
     * Joins the space the invitation with this [code] invites to, as a plain member, and returns its id. Needs
     * a connection: the server decides whether the invitation still works. Throws a [JoinException] when the
     * invitation refuses and any other exception when the join itself fails.
     */
    suspend fun joinSpace(code: String): String

    /**
     * Changes the name and the colour of a space. A null argument is left as it is, and only the fields given are
     * written, so an edit never overwrites a change someone else made to the other one. Only an owner or admin
     * can. Throws when it is not allowed, when the name is blank or when nothing is given.
     */
    suspend fun updateSpace(spaceId: String, name: String?, accent: Int?)
}
