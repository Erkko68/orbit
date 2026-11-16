package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class NextTurnTest {
    private val day = LocalDate(2026, 11, 20)

    private fun member(id: String, awayFrom: LocalDate? = null, awayUntil: LocalDate? = null) =
        Member(id, id, photoUrl = null, role = Role.MEMBER, awayFrom = awayFrom, awayUntil = awayUntil)

    private fun everyone(vararg ids: String) = ids.map { member(it) }

    private fun rotation(current: String, vararg order: String) = Assignment.Rotation(order.toList(), current)

    @Test
    fun theTurnGoesToTheNextMemberInTheOrder() {
        val members = everyone("ana", "ben", "cy")
        assertEquals("ben", nextTurn(rotation("ana", "ana", "ben", "cy"), members, day))
        assertEquals("cy", nextTurn(rotation("ben", "ana", "ben", "cy"), members, day))
    }

    @Test
    fun theLastMemberPassesItBackToTheFirst() {
        assertEquals("ana", nextTurn(rotation("cy", "ana", "ben", "cy"), everyone("ana", "ben", "cy"), day))
    }

    @Test
    fun theOrderIsTheRotationsOwnAndNotTheMembersList() {
        val members = everyone("ana", "ben", "cy")
        assertEquals("ana", nextTurn(rotation("ben", "cy", "ben", "ana"), members, day))
    }

    @Test
    fun aMemberAwayOnTheDayIsSkipped() {
        val members = listOf(member("ana"), member("ben", day, day), member("cy"))
        assertEquals("cy", nextTurn(rotation("ana", "ana", "ben", "cy"), members, day))
    }

    @Test
    fun anAwayPeriodCountsOnItsFirstAndLastDayAndNotOutsideIt() {
        val period = member("ben", LocalDate(2026, 11, 20), LocalDate(2026, 11, 22))
        val order = rotation("ana", "ana", "ben", "cy")
        val members = listOf(member("ana"), period, member("cy"))
        assertEquals("cy", nextTurn(order, members, LocalDate(2026, 11, 20)))
        assertEquals("cy", nextTurn(order, members, LocalDate(2026, 11, 22)))
        assertEquals("ben", nextTurn(order, members, LocalDate(2026, 11, 19)))
        assertEquals("ben", nextTurn(order, members, LocalDate(2026, 11, 23)))
    }

    @Test
    fun severalMembersAwayAreAllSkipped() {
        val members = listOf(member("ana"), member("ben", day, day), member("cy", day, day), member("di"))
        assertEquals("di", nextTurn(rotation("ana", "ana", "ben", "cy", "di"), members, day))
    }

    @Test
    fun whenEveryoneElseIsAwayTheCurrentMemberKeepsIt() {
        val members = listOf(member("ana"), member("ben", day, day), member("cy", day, day))
        assertEquals("ana", nextTurn(rotation("ana", "ana", "ben", "cy"), members, day))
    }

    @Test
    fun whenEveryoneIsAwayTheCurrentMemberKeepsIt() {
        val members = listOf(member("ana", day, day), member("ben", day, day))
        assertEquals("ana", nextTurn(rotation("ana", "ana", "ben"), members, day))
    }

    @Test
    fun aMemberWhoLeftIsSkipped() {
        val members = everyone("ana", "cy")
        assertEquals("cy", nextTurn(rotation("ana", "ana", "ben", "cy"), members, day))
    }

    @Test
    fun whenTheCurrentMemberLeftTheNextInTheOrderTakesOver() {
        val members = everyone("ana", "cy")
        assertEquals("cy", nextTurn(rotation("ben", "ana", "ben", "cy"), members, day))
    }

    @Test
    fun whenTheCurrentMemberLeftAndEveryoneIsAwayTheNextInTheOrderStillTakesOver() {
        val members = listOf(member("ana", day, day), member("cy", day, day))
        assertEquals("cy", nextTurn(rotation("ben", "ana", "ben", "cy"), members, day))
    }

    @Test
    fun withNobodyLeftNothingChanges() {
        assertEquals("ana", nextTurn(rotation("ana", "ana", "ben"), emptyList(), day))
    }

    @Test
    fun aCurrentMemberOutsideTheOrderStartsItFromTheFirst() {
        assertEquals("ana", nextTurn(rotation("zoe", "ana", "ben"), everyone("ana", "ben", "zoe"), day))
    }

    @Test
    fun everyMemberGetsATurnBeforeAnyoneGetsASecond() {
        val members = everyone("ana", "ben", "cy")
        var turn = rotation("ana", "ana", "ben", "cy")
        val seen = List(6) {
            nextTurn(turn, members, day).also { turn = turn.copy(current = it) }
        }
        assertEquals(listOf("ben", "cy", "ana", "ben", "cy", "ana"), seen)
    }
}
