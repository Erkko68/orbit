package com.orbit.app.feature.member.away

import com.orbit.app.domain.model.isValidAway
import kotlinx.datetime.LocalDate

/** Which end of the period the date picker is choosing. */
enum class AwayEnd { FROM, UNTIL }

/** The user's own away period in a space, and the sheet that sets it (#48). */
data class AwayUiState(
    /** The member has not arrived yet. */
    val loading: Boolean = true,
    /** Loading the member failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    /** The saved period, while it is running or still ahead. A finished one is not shown. */
    val from: LocalDate? = null,
    val until: LocalDate? = null,
    /** The sheet is open. */
    val editing: Boolean = false,
    /** The days the sheet shows: the saved ones, or today while there are none. */
    val draftFrom: LocalDate? = null,
    val draftUntil: LocalDate? = null,
    /** The end the date picker is choosing, while it is open. */
    val picking: AwayEnd? = null,
    /** A save or a clear is under way. */
    val busy: Boolean = false,
    /** The last of those failed. Cleared by the next edit or attempt. */
    val failed: Boolean = false,
) {
    val isAway: Boolean get() = from != null && until != null

    val canSave: Boolean
        get() = !busy && draftFrom != null && draftUntil != null && isValidAway(draftFrom, draftUntil)

    /** The picker's selected day. */
    val pickedDay: LocalDate?
        get() = when (picking) {
            AwayEnd.FROM -> draftFrom
            AwayEnd.UNTIL -> draftUntil
            null -> null
        }

    /** The first day the picker offers: the last day cannot come before the first. */
    val earliestDay: LocalDate? get() = if (picking == AwayEnd.UNTIL) draftFrom else null
}
