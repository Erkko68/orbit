package com.orbit.app.feature.member

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.jetbrains.compose.resources.StringResource

/** One member of a space: who they are, what they have and what they did lately. */
data class MemberUiState(
    val loading: Boolean = false,
    /** Reading the members failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    /** They are not, or no longer, in the space. */
    val notFound: Boolean = false,
    val name: String = "",
    val isYou: Boolean = false,
    val initials: String = "",
    val photoUrl: String? = null,
    /** Their place in the space, which picks their colour and shape. */
    val index: Int = 0,
    val role: StringResource? = null,
    val spaceName: String = "",
    val today: LocalDate? = null,
    /** Their open items, the soonest first. */
    val has: List<MemberItem> = emptyList(),
    /** What they completed lately, newest first. */
    val lately: List<MemberCompletion> = emptyList(),
) {
    val loaded: Boolean get() = !loading && !loadFailed && !notFound
    val hasNothing: Boolean get() = has.isEmpty() && lately.isEmpty()
}

data class MemberItem(val id: String, val title: String, val due: LocalDate?)

/** A completion on the phone's clock. */
data class MemberCompletion(val id: String, val itemId: String, val title: String, val date: LocalDate, val time: LocalTime)
