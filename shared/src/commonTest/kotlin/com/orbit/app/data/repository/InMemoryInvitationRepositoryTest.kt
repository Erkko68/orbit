package com.orbit.app.data.repository

import com.orbit.app.domain.model.INVITATION_CODE_LENGTH
import com.orbit.app.domain.model.INVITATION_VALIDITY
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class InMemoryInvitationRepositoryTest {
    private val auth = InMemoryAuthRepository()

    private class SteppingClock(private var now: Instant) : Clock {
        override fun now(): Instant = now.also { now += 1.seconds }
    }

    @Test
    fun createsAnInvitationOfTheSignedInUserThatLastsAWeek() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val now = Instant.fromEpochSeconds(5_000)
        val repository = InMemoryInvitationRepository(auth, SteppingClock(now))

        val code = repository.createInvitation("space-1")

        val invitation = repository.observeInvitations("space-1").first().single()
        assertEquals(code, invitation.code)
        assertEquals(INVITATION_CODE_LENGTH, code.length)
        assertEquals("space-1", invitation.spaceId)
        assertEquals("ana@orbit.app", invitation.createdBy)
        assertEquals(now + INVITATION_VALIDITY, invitation.expiresAt)
        assertEquals(false, invitation.revoked)
    }

    @Test
    fun needsASignedInUser() = runTest {
        assertFailsWith<IllegalStateException> { InMemoryInvitationRepository(auth).createInvitation("space-1") }
    }

    @Test
    fun listsTheInvitationsOfEachSpaceNewestFirst() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val repository = InMemoryInvitationRepository(auth, SteppingClock(Instant.fromEpochSeconds(5_000)))
        val first = repository.createInvitation("space-1")
        repository.createInvitation("space-2")
        val third = repository.createInvitation("space-1")

        assertEquals(listOf(third, first), repository.observeInvitations("space-1").first().map { it.code })
        assertEquals(1, repository.observeInvitations("space-2").first().size)
        assertTrue(repository.observeInvitations("space-3").first().isEmpty())
    }

    @Test
    fun revokesOneInvitationAndLeavesTheOthers() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val repository = InMemoryInvitationRepository(auth, SteppingClock(Instant.fromEpochSeconds(5_000)))
        val kept = repository.createInvitation("space-1")
        val revoked = repository.createInvitation("space-1")

        repository.revokeInvitation(revoked)

        val byCode = repository.observeInvitations("space-1").first().associateBy { it.code }
        assertEquals(true, byCode.getValue(revoked).revoked)
        assertEquals(false, byCode.getValue(kept).revoked)
    }
}
