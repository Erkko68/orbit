package com.orbit.app.testing

import com.orbit.app.data.repository.requireUser
import com.orbit.app.domain.model.INVITATION_CODE_LENGTH
import com.orbit.app.domain.model.INVITATION_VALIDITY
import com.orbit.app.domain.model.Invitation
import com.orbit.app.domain.model.invitationCodeFrom
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.InvitationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/** Invitations in memory. Codes are made in order, so each test gets the same ones. */
class InMemoryInvitationRepository(
    private val auth: AuthRepository,
    private val clock: Clock = Clock.System,
) : InvitationRepository {
    private val invitations = MutableStateFlow(emptyList<Invitation>())

    override fun observeInvitations(spaceId: String): Flow<List<Invitation>> = invitations.map { all ->
        all.filter { it.spaceId == spaceId }.sortedByDescending { it.createdAt }
    }

    override suspend fun createInvitation(spaceId: String): String {
        val user = auth.requireUser()
        val now = clock.now()
        val code = invitationCodeFrom("invite${invitations.value.size}".padStart(INVITATION_CODE_LENGTH, '0'))
        invitations.update { it + Invitation(code, spaceId, user.id, now, now + INVITATION_VALIDITY, revoked = false) }
        return code
    }

    /** The invitation with this code, whatever its space and state: what joining looks up. */
    fun find(code: String): Invitation? = invitations.value.firstOrNull { it.code == code }

    override suspend fun revokeInvitation(code: String) {
        invitations.update { all -> all.map { if (it.code == code) it.copy(revoked = true) else it } }
    }
}
