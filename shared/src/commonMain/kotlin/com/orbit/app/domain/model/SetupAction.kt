package com.orbit.app.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime

/** An action proposed by the setup assistant. Never applied without user confirmation. */
sealed interface SetupAction {
    data class CreateSpace(val name: String) : SetupAction
    data class AddMember(val name: String) : SetupAction
    data class AddChore(val title: String, val every: DatePeriod, val rotation: List<String>) : SetupAction
    data class AddEvent(val title: String, val start: LocalDateTime) : SetupAction
    data class AddRecurringExpense(val title: String, val amountCents: Long, val every: DatePeriod) : SetupAction
}
