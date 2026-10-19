package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.mapMember
import com.orbit.app.data.mapper.mapSpace
import com.orbit.app.data.mapper.ownerMemberDocument
import com.orbit.app.data.mapper.spaceDocument
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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

    private fun toSpace(document: DocumentSnapshot): Space? =
        mapSpace(document.id, document.field("name"), document.field("accent"), document.field("memberIds")).also {
            if (it == null) Logger.w { "Skipping the malformed space ${document.id}" }
        }
}
