package com.orbit.app.testing

import com.orbit.app.domain.model.Invitation
import com.orbit.app.domain.repository.InvitationRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Invitations that fail to create, to revoke, or even to load, as the Firestore ones do without a connection.
 * With [neverLoad] the listener stays silent, as one waiting for the server does.
 */
internal class FailingInvitationRepository(
    private val invitations: List<Invitation> = emptyList(),
    private val failToLoad: Boolean = false,
    private val neverLoad: Boolean = false,
) : InvitationRepository {
    override fun observeInvitations(spaceId: String): Flow<List<Invitation>> = when {
        failToLoad -> flow { error("Could not reach the server") }
        neverLoad -> flow { awaitCancellation() }
        else -> flowOf(invitations)
    }

    override suspend fun createInvitation(spaceId: String): String = error("Could not reach the server")

    override suspend fun revokeInvitation(code: String) = error("Could not reach the server")
}
