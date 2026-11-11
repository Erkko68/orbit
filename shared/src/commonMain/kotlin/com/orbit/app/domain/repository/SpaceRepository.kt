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
     * role of the owner never changes, and nobody becomes owner this way: ownership moves only through
     * [transferOwnership]. Throws when it is not allowed.
     */
    suspend fun changeRole(spaceId: String, userId: String, role: Role)

    /**
     * Joins the space the invitation with this [code] invites to, as a plain member, and returns its id. Needs
     * a connection: the server decides whether the invitation still works. Throws a [JoinException] when the
     * invitation refuses and any other exception when the join itself fails.
     */
    suspend fun joinSpace(code: String): String

    /** Renames a space. Only an owner or admin can. Throws when it is not allowed or when the name is blank. */
    suspend fun renameSpace(spaceId: String, name: String)

    /**
     * Takes the signed-in user out of the space. The owner cannot leave: they delete the space with [deleteSpace],
     * or hand it over with [transferOwnership] first (ADR 0004). Throws when it is not allowed.
     */
    suspend fun leaveSpace(spaceId: String)

    /**
     * Takes [userId], another member, out of the space. The owner removes anyone else; an admin removes a plain
     * member; the owner is never removed. The space keeps [displayName] among its former members, which is what
     * its history shows, so the caller passes the name it already has. Throws when it is not allowed.
     */
    suspend fun removeMember(spaceId: String, userId: String, displayName: String)

    /** Deletes the space for every member. Only the owner can. Throws when it is not allowed. */
    suspend fun deleteSpace(spaceId: String)

    /** Makes [userId], another member, the owner, and the signed-in owner an admin. Throws when it is not allowed. */
    suspend fun transferOwnership(spaceId: String, userId: String)
}
