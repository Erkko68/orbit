package com.orbit.app.feature.activity

import com.orbit.app.domain.model.ActivityAction
import com.orbit.app.domain.model.ActivityTarget
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** What changed in a space in the last 30 days (#83), newest first, by day on the phone's clock. */
data class ActivityUiState(
    /** The history has not arrived yet. */
    val loading: Boolean = false,
    /** Loading it failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    val days: List<ActivityDay> = emptyList(),
) {
    /** Loaded, and nothing changed in the window. */
    val isEmpty: Boolean get() = !loading && !loadFailed && days.isEmpty()
}

/** The entries of one [date], newest first. [relative] names today and yesterday instead of their date. */
data class ActivityDay(val date: LocalDate, val relative: RelativeDay, val entries: List<ActivityEntry>)

enum class RelativeDay { TODAY, YESTERDAY, EARLIER }

/**
 * One change: [actor] did [action] to [title]. [actor] is null when the history names someone the space no longer
 * knows, and [byYou] when it was the user.
 */
data class ActivityEntry(
    val id: String,
    val action: ActivityAction,
    val target: ActivityTarget,
    val title: String,
    val actor: String?,
    val byYou: Boolean,
    val time: LocalTime,
    val byAssistant: Boolean,
)
