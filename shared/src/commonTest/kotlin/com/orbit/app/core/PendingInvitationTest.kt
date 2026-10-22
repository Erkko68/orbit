package com.orbit.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingInvitationTest {
    private val pending = PendingInvitation()

    @Test
    fun hasNothingWaitingAtFirst() {
        assertNull(pending.code.value)
    }

    @Test
    fun keepsTheCodeOfALinkUntilItIsTaken() {
        pending.offer("orbit://join/abcd2345")

        assertEquals("ABCD2345", pending.code.value)

        pending.consume()

        assertNull(pending.code.value)
    }

    @Test
    fun ignoresALinkWithoutACode() {
        pending.offer("orbit://join/")
        pending.offer("https://example.com/")

        assertNull(pending.code.value)
    }

    @Test
    fun aNewLinkReplacesTheOneStillWaiting() {
        pending.offer("orbit://join/ABCD2345")
        pending.offer("orbit://join/WXYZ6789")

        assertEquals("WXYZ6789", pending.code.value)
    }

    @Test
    fun aLinkWithoutACodeLeavesTheWaitingOneAlone() {
        pending.offer("orbit://join/ABCD2345")
        pending.offer("orbit://join/")

        assertEquals("ABCD2345", pending.code.value)
    }
}
