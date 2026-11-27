package com.orbit.app.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.orOnFailure
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Activity
import com.orbit.app.domain.repository.ActivityRepository
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * The Activity screen of a space (#83): its history of the last 30 days, grouped by day. The history is what the
 * screen is about, so a failed read shows with a retry. Who did each change only decorates it: the members, or the
 * name a former member left in the space, and without them the entry still shows.
 */
class ActivityViewModel(
    private val spaceId: String,
    activity: ActivityRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val zone = TimeZone.currentSystemDefault()
    private val retries = Channel<Unit>(Channel.CONFLATED)

    private val names: Flow<Map<String, String>> = combine(
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList())
            .onStart { emit(emptyList()) },
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()).onStart { emit(emptyList()) },
    ) { members, all ->
        all.firstOrNull { it.id == spaceId }?.formerMembers.orEmpty() + members.associate { it.userId to it.displayName }
    }

    val uiState: StateFlow<ActivityUiState> = combine(
        activity.observeActivity(spaceId, Clock.System.now() - WINDOW)
            .asLoad("Loading the activity of $spaceId", retries),
        names,
        auth.session,
    ) { load, names, user ->
        val today = Clock.System.todayIn(zone)
        ActivityUiState(
            loading = load is Load.Loading,
            loadFailed = load is Load.Failed,
            days = load.valueOrNull.orEmpty().sortedByDescending { it.at }
                .groupBy { it.at.toLocalDateTime(zone).date }
                .map { (date, day) -> ActivityDay(date, relativeTo(date, today), day.map { it.toEntry(names, user?.id) }) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState(loading = true))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    private fun relativeTo(date: LocalDate, today: LocalDate): RelativeDay = when (date) {
        today -> RelativeDay.TODAY
        today.minus(DatePeriod(days = 1)) -> RelativeDay.YESTERDAY
        else -> RelativeDay.EARLIER
    }

    private fun Activity.toEntry(names: Map<String, String>, userId: String?): ActivityEntry {
        val moment = at.toLocalDateTime(zone)
        return ActivityEntry(
            id = id,
            action = action,
            target = target,
            title = title,
            actor = names[actorId],
            byYou = actorId == userId,
            time = LocalTime(moment.hour, moment.minute),
            byAssistant = byAssistant,
        )
    }

    private companion object {
        val WINDOW = 30.days
    }
}
