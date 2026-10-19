package com.orbit.app.data.mapper

import com.orbit.app.domain.model.Invitation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class InvitationMapperTest {
    private val created = Instant.fromEpochSeconds(1_000)
    private val expires = Instant.fromEpochSeconds(2_000)

    @Test
    fun mapsAnInvitation() {
        val invitation = mapInvitation("ABCD2345", "space-1", "ana", created, expires, revoked = true)

        assertEquals(Invitation("ABCD2345", "space-1", "ana", created, expires, revoked = true), invitation)
    }

    @Test
    fun anInvitationWithoutTheRevokedFlagIsNotRevoked() {
        assertEquals(false, mapInvitation("ABCD2345", "space-1", "ana", created, expires, revoked = null)?.revoked)
    }

    @Test
    fun skipsAnInvitationMissingAnythingItCannotDoWithout() {
        assertNull(mapInvitation("ABCD2345", null, "ana", created, expires, false))
        assertNull(mapInvitation("ABCD2345", "space-1", null, created, expires, false))
        assertNull(mapInvitation("ABCD2345", "space-1", "ana", null, expires, false))
        assertNull(mapInvitation("ABCD2345", "space-1", "ana", created, null, false))
    }
}
