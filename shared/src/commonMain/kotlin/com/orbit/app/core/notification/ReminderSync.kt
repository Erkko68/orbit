package com.orbit.app.core.notification

import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Keeps this phone's reminders in step with the signed-in user's open items in every space. An edit, here or
 * on another member's phone, reschedules them; completing or deleting the item, removing its reminder or
 * signing out cancels them. An item assigned to someone else does not remind this user. What is scheduled is
 * kept in [settings], so what changed while the app was closed is caught up on the next launch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSync(
    private val auth: AuthRepository,
    private val spaces: SpaceRepository,
    private val items: ItemRepository,
    private val scheduler: NotificationScheduler,
    private val settings: Settings,
) {
    /** Syncs until cancelled. A failed read leaves the reminders as they are and tries again later. */
    suspend fun run() {
        auth.session.map { it?.id }.distinctUntilChanged()
            .flatMapLatest { userId -> if (userId == null) flowOf(emptyMap()) else wanted(userId) }
            .retryWhen { cause, _ ->
                Logger.e(cause) { "Syncing reminders failed" }
                delay(RETRY_DELAY)
                true
            }
            .collect(::apply)
    }

    private fun wanted(userId: String): Flow<Map<String, Scheduled>> = spaces.observeSpaces().flatMapLatest { all ->
        // combine() of no flows never emits.
        if (all.isEmpty()) return@flatMapLatest flowOf(emptyMap())
        combine(
            all.map { space ->
                items.observeItems(space.id).map { list -> list.flatMap { it.reminders(space.name, userId) } }
            },
        ) { perSpace -> perSpace.flatMap { it }.toMap() }
    }

    private fun Item.reminders(spaceName: String, userId: String): List<Pair<String, Scheduled>> {
        val schedule = modules.schedule
        val offsets = modules.reminder?.offsets
        val assignee = modules.assignment?.assigneeId
        if (status != ItemStatus.Open || schedule == null || offsets == null) return emptyList()
        if (assignee != null && assignee != userId) return emptyList()
        val due = schedule.dueMoment(TimeZone.currentSystemDefault())
        return offsets.map { offset ->
            "reminder-$id-${offset.inWholeMinutes}" to Scheduled((due - offset).toEpochMilliseconds(), title, spaceName)
        }
    }

    /**
     * Schedules only what changed. A reminder whose time has passed is not scheduled again, and one that
     * already showed stays on screen until its item is done.
     */
    private fun apply(wanted: Map<String, Scheduled>) {
        val scheduled = settings.getStringOrNull(KEY)
            ?.let { Json.decodeFromString<Map<String, Scheduled>>(it) }
            .orEmpty()
        val now = Clock.System.now().toEpochMilliseconds()
        (scheduled.keys - wanted.keys).forEach(scheduler::cancel)
        for ((id, reminder) in wanted) {
            if (scheduled[id] == reminder) continue
            if (reminder.atMillis > now) {
                scheduler.schedule(id, Instant.fromEpochMilliseconds(reminder.atMillis), reminder.title, reminder.body)
            } else {
                scheduler.cancel(id)
            }
        }
        settings.putString(KEY, Json.encodeToString(wanted))
    }

    @Serializable
    private data class Scheduled(val atMillis: Long, val title: String, val body: String)

    private companion object {
        const val KEY = "scheduled_reminders"
        val RETRY_DELAY = 30.seconds
    }
}
