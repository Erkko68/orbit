package com.orbit.app.domain.model

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class InvitationTest {
    private val created = Instant.fromEpochSeconds(1_000_000)
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private fun invitation(revoked: Boolean = false) = Invitation(
        code = "ABCD2345",
        spaceId = "space-1",
        createdBy = "ana",
        createdAt = created,
        expiresAt = created + INVITATION_VALIDITY,
        revoked = revoked,
    )

    @Test
    fun worksUntilItExpires() {
        assertTrue(invitation().isActive(created))
        assertTrue(invitation().isActive(created + 6.days))
        assertFalse(invitation().isActive(created + INVITATION_VALIDITY))
        assertFalse(invitation().isActive(created + 8.days))
    }

    @Test
    fun stopsWorkingOnceRevoked() {
        assertFalse(invitation(revoked = true).isActive(created))
    }

    @Test
    fun hasALinkThatCarriesTheCode() {
        assertEquals("orbit://join/ABCD2345", invitation().link)
        assertEquals("orbit://join/ABCD2345", invitationLink("ABCD2345"))
    }

    @Test
    fun aCodeUsesOnlyUnambiguousCharacters() {
        val random = Random(7)
        repeat(200) {
            val code = newInvitationCode(random)
            assertEquals(INVITATION_CODE_LENGTH, code.length)
            assertTrue(code.all { it in alphabet }, code)
        }
    }

    @Test
    fun aCodeIsMadeOfTheFirstCharactersOfARandomId() {
        val fromId = invitationCodeFrom("AbC09xYz1234567890Qw")

        assertEquals(INVITATION_CODE_LENGTH, fromId.length)
        assertTrue(fromId.all { it in alphabet }, fromId)
        assertEquals(fromId, invitationCodeFrom("AbC09xYzDifferentTail"))
    }

    @Test
    fun aCodeNeedsEnoughRandomCharacters() {
        assertFailsWith<IllegalArgumentException> { invitationCodeFrom("short") }
    }
}
