package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemberAwayTest {
    private val from = LocalDate(2026, 11, 10)
    private val until = LocalDate(2026, 11, 14)
    private fun member(from: LocalDate? = this.from, until: LocalDate? = this.until) =
        Member("ana", "Ana", null, Role.MEMBER, from, until)

    @Test
    fun `is away on every day of the period with both ends included`() {
        assertTrue(member().isAway(from))
        assertTrue(member().isAway(LocalDate(2026, 11, 12)))
        assertTrue(member().isAway(until))
    }

    @Test
    fun `is not away before or after the period`() {
        assertFalse(member().isAway(LocalDate(2026, 11, 9)))
        assertFalse(member().isAway(LocalDate(2026, 11, 15)))
    }

    @Test
    fun `is never away without both ends`() {
        assertFalse(member(until = null).isAway(from))
        assertFalse(member(from = null).isAway(until))
        assertFalse(Member("ana", "Ana", null, Role.MEMBER).isAway(from))
    }

    @Test
    fun `shows the last day while the period runs or is ahead and not once it is over`() {
        assertEquals(until, member().awayUntilFrom(LocalDate(2026, 11, 1)))
        assertEquals(until, member().awayUntilFrom(until))
        assertNull(member().awayUntilFrom(LocalDate(2026, 11, 15)))
        assertNull(member(from = null).awayUntilFrom(from))
    }

    @Test
    fun `a period ends on or after the day it starts`() {
        assertTrue(isValidAway(from, from))
        assertTrue(isValidAway(from, until))
        assertFalse(isValidAway(until, from))
    }
}
