package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Member
import kotlinx.datetime.LocalDate

/**
 * Whose turn it is after [rotation]'s current one, for the occurrence that falls on [on] (ADR 0015): the next member
 * in the order who is still in the space and not away that day. Members who left are skipped. When everyone else is
 * away the current member keeps it, and when the current member left too, the next member in the order has it
 * whether away or not. With nobody left in the space, nobody changes.
 */
fun nextTurn(rotation: Assignment.Rotation, members: List<Member>, on: LocalDate): String {
    val inSpace = members.associateBy { it.userId }
    val order = rotation.order
    val position = order.indexOf(rotation.current)
    val after = List(order.size) { order[(position + 1 + it) % order.size] }
        .let { if (position < 0) order else it }
        .filter { it in inSpace }
    return after.firstOrNull { inSpace.getValue(it).isAway(on).not() }
        ?: rotation.current.takeIf { it in inSpace }
        ?: after.firstOrNull()
        ?: rotation.current
}
