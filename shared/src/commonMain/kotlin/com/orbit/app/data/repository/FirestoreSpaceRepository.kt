package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.joiningMemberDocument
import com.orbit.app.data.mapper.mapInvitation
import com.orbit.app.data.mapper.mapMember
import com.orbit.app.data.mapper.mapSpace
import com.orbit.app.data.mapper.ownerMemberDocument
import com.orbit.app.data.mapper.roleValue
import com.orbit.app.data.mapper.spaceDocument
import com.orbit.app.domain.model.JoinError
import com.orbit.app.domain.model.JoinException
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Spaces and their members in Firestore (ADR 0002), read through live listeners. Firestore's offline
 * cache answers them first, so they work offline.
 */
class FirestoreSpaceRepository(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
) : SpaceRepository {
    /** The spaces whose `memberIds` contain the signed-in user's uid. Empty when signed out. */
    override fun observeSpaces(): Flow<List<Space>> = channelFlow {
        auth.session.map { it?.id }.distinctUntilChanged().collectLatest { uid ->
            if (uid == null) {
                send(emptyList())
            } else {
                firestore.collection(SPACES).where { "memberIds" contains uid }.snapshots.collect { snapshot ->
                    send(snapshot.documents.mapNotNull(::toSpace))
                }
            }
        }
    }

    /**
     * The members of a space, live. A space made a moment ago is already in the list from the local write,
     * so its card can ask for the members before the server has the space, and the rules refuse that. The
     * listener asks again for a few seconds before it fails like any other read.
     */
    override fun observeMembers(spaceId: String): Flow<List<Member>> =
        firestore.collection(SPACES).document(spaceId).collection(MEMBERS).snapshots.map { snapshot ->
            snapshot.documents.map { document ->
                mapMember(
                    userId = document.id,
                    displayName = document.field("displayName"),
                    photoUrl = document.field("photoUrl"),
                    role = document.field("role"),
                )
            }
        }.retryWhileRefused()

    /**
     * The space and its owner's membership go in one batch, so the member ids and the members
     * subcollection cannot drift. The id is made on the device, so this works offline ([writeLocally]).
     */
    override suspend fun createSpace(name: String, accent: Int): String {
        val user = auth.requireUser()
        val space = firestore.collection(SPACES).document
        val batch = firestore.batch()
        batch.set(space, spaceDocument(name, accent, user.id))
        batch.set(space.collection(MEMBERS).document(user.id), ownerMemberDocument(user.displayName, user.photoUrl))
        writeLocally("Space ${space.id}") { batch.commit() }
        return space.id
    }

    /** Writes only the role, so a change never overwrites the profile copy next to it. The rules decide who may. */
    override suspend fun changeRole(spaceId: String, userId: String, role: Role) {
        require(role != Role.OWNER) { "Ownership moves only through a transfer" }
        val member = firestore.collection(SPACES).document(spaceId).collection(MEMBERS).document(userId)
        writeLocally("Role of $userId in space $spaceId") { member.update("role" to roleValue(role)) }
    }

    /**
     * The batch is the one the rules accept for a join (ADR 0002): the caller added to `memberIds` and their
     * member document naming the invitation. The invitation is read from the server, so one revoked a moment ago
     * is not trusted from the cache, and the write is awaited: joining needs the server's answer, so it cannot
     * queue offline and surprise the user later.
     */
    override suspend fun joinSpace(code: String): String {
        val user = auth.requireUser()
        val document = firestore.collection(INVITATIONS).document(code).get(Source.SERVER)
        val invitation = mapInvitation(
            code = code,
            spaceId = document.field("spaceId"),
            createdBy = document.field("createdBy"),
            createdAt = document.instant("createdAt"),
            expiresAt = document.instant("expiresAt"),
            revoked = document.field("revoked"),
        )
        if (!document.exists || invitation == null) throw JoinException(JoinError.INVALID)
        if (invitation.revoked) throw JoinException(JoinError.REVOKED)
        if (!invitation.isActive(Clock.System.now())) throw JoinException(JoinError.EXPIRED)
        val mine = firestore.collection(SPACES).where { "memberIds" contains user.id }.get().documents
        if (mine.any { it.id == invitation.spaceId }) throw JoinException(JoinError.ALREADY_MEMBER)

        val space = firestore.collection(SPACES).document(invitation.spaceId)
        val batch = firestore.batch()
        batch.update(space, "memberIds" to FieldValue.arrayUnion(user.id))
        val membership = joiningMemberDocument(user.displayName, user.photoUrl, code)
        batch.set(space.collection(MEMBERS).document(user.id), membership)
        try {
            withTimeout(JOIN_WAIT) { batch.commit() }
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("The server did not confirm joining the space in time", e)
        }
        return invitation.spaceId
    }

    private fun toSpace(document: DocumentSnapshot): Space? =
        mapSpace(document.id, document.field("name"), document.field("accent"), document.field("memberIds")).also {
            if (it == null) Logger.w { "Skipping the malformed space ${document.id}" }
        }
}

private val JOIN_WAIT = 15.seconds
