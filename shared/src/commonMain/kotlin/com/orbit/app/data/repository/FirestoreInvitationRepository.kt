package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.mapper.invitationDocument
import com.orbit.app.data.mapper.mapInvitation
import com.orbit.app.domain.model.Invitation
import com.orbit.app.domain.model.invitationCodeFrom
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.InvitationRepository
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Invitations in Firestore (ADR 0002): one top-level document per invitation, whose id is its code. The
 * rules let only an owner or admin of the space create, list and revoke them.
 */
class FirestoreInvitationRepository(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
) : InvitationRepository {
    /**
     * The invitations of the space, live. The rules answer this query for the owner and admins, and
     * their membership of a space made a moment ago may not be on the server yet, so a refusal is tried
     * again for a few seconds. If it stays refused the flow ends, logged, instead of crashing.
     */
    override fun observeInvitations(spaceId: String): Flow<List<Invitation>> =
        firestore.collection(INVITATIONS).where { "spaceId" equalTo spaceId }.snapshots
            .map { snapshot -> snapshot.documents.mapNotNull(::toInvitation).sortedByDescending { it.createdAt } }
            .retryWhileRefused(LISTEN_RETRIES, LISTEN_RETRY_WAIT, Throwable::isRefusedByTheRules)
            .catch { cause ->
                if (!cause.isRefusedByTheRules()) throw cause
                Logger.w(cause) { "The invitations of space $spaceId are not readable" }
            }

    override suspend fun createInvitation(spaceId: String): String {
        val userId = auth.requireUser().id
        // The SDK draws ids from a secure generator on the device, so the code cannot be guessed and
        // making it needs no connection.
        val code = invitationCodeFrom(firestore.collection(INVITATIONS).document.id)
        val data = invitationDocument(spaceId, userId, Clock.System.now())
        writeLocally("Invitation $code") { firestore.collection(INVITATIONS).document(code).set(data) }
        return code
    }

    override suspend fun revokeInvitation(code: String) {
        writeLocally("Revoking invitation $code") {
            firestore.collection(INVITATIONS).document(code).update(REVOKED to true)
        }
    }

    private fun toInvitation(document: DocumentSnapshot): Invitation? =
        mapInvitation(
            code = document.id,
            spaceId = document.field("spaceId"),
            createdBy = document.field("createdBy"),
            createdAt = document.instant("createdAt"),
            expiresAt = document.instant("expiresAt"),
            revoked = document.field(REVOKED),
        ).also {
            if (it == null) Logger.w { "Skipping the malformed invitation ${document.id}" }
        }

    private companion object {
        const val INVITATIONS = "invitations"
        const val REVOKED = "revoked"
        const val LISTEN_RETRIES = 5
        val LISTEN_RETRY_WAIT = 1.seconds
    }
}
