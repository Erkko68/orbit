package com.orbit.app.feature.spacehome.members

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** What one member of a space has and what they did lately. Who they are comes from the members already listed. */
data class MemberActivityUiState(
    /** Their items have not arrived yet. */
    val loading: Boolean = false,
    val today: LocalDate? = null,
    /** Their open items, the soonest first. */
    val has: List<MemberItem> = emptyList(),
    /** What they completed lately, newest first. */
    val lately: List<MemberCompletion> = emptyList(),
) {
    val hasNothing: Boolean get() = !loading && has.isEmpty() && lately.isEmpty()
}

data class MemberItem(val id: String, val title: String, val due: LocalDate?)

/** A completion on the phone's clock. */
data class MemberCompletion(val id: String, val itemId: String, val title: String, val date: LocalDate, val time: LocalTime)
