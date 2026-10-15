package com.orbit.app.data.mapper

import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlin.time.Duration.Companion.minutes

internal const val STATUS_OPEN = "open"
internal const val STATUS_DONE = "done"

private val HOUR_MINUTE = LocalTime.Format {
    hour()
    char(':')
    minute()
}

/** The key of a module in the `modules` map. */
internal fun moduleKey(type: ModuleType): String = type.name.lowercase()

/** The new item, open, made by [creatorId]. */
internal fun itemDocument(spaceId: String, item: NewItem, creatorId: String) = ItemDocument(
    spaceId = spaceId,
    title = item.title,
    description = item.description,
    status = STATUS_OPEN,
    preset = item.preset?.name?.lowercase(),
    createdBy = creatorId,
    createdAt = Timestamp.ServerTimestamp,
    modules = ModulesDocument(
        schedule = item.modules.schedule?.let(::scheduleDocument),
        assignment = item.modules.assignment?.let(::assignmentDocument),
        checklist = item.modules.checklist?.let(::checklistDocument),
        reminder = item.modules.reminder?.let(::reminderDocument),
    ),
)

internal fun scheduleDocument(schedule: Schedule) = ScheduleDocument(
    date = schedule.date.toString(),
    time = schedule.time?.format(HOUR_MINUTE),
    endDate = schedule.endDate?.toString(),
    durationMinutes = schedule.duration?.inWholeMinutes?.toInt(),
)

internal fun assignmentDocument(assignment: Assignment) = when (assignment) {
    is Assignment.ToMember -> AssignmentDocument("member", assignment.assigneeId)
    Assignment.Anyone -> AssignmentDocument("anyone")
    is Assignment.Claim -> AssignmentDocument("claim", assignment.assigneeId)
}

/** Entries keyed by id, their list position kept in `order`. */
internal fun checklistDocument(checklist: Checklist) = ChecklistDocument(
    checklist.entries.withIndex().associate { (order, entry) ->
        entry.id to EntryDocument(entry.text, entry.checked, entry.quantity, order)
    },
)

internal fun reminderDocument(reminder: Reminder) =
    ReminderDocument(reminder.offsets.map { it.inWholeMinutes.toInt() })

/**
 * An item from its stored fields, or null when it has no title, creator or creation time: a malformed
 * document is skipped by the caller. What a newer build may have written (an unknown preset, module or
 * assignment mode) is skipped and logged, so the rest of the item still shows.
 */
internal fun mapItem(id: String, spaceId: String, stored: StoredItem): Item? {
    val title = stored.title ?: return null
    val createdBy = stored.createdBy ?: return null
    val createdAt = stored.createdAt ?: return null
    return Item(
        id = id,
        spaceId = spaceId,
        title = title,
        description = stored.description,
        status = statusOf(stored),
        preset = stored.preset?.let { presetOf(id, it) },
        createdBy = createdBy,
        createdAt = createdAt,
        modules = modulesOf(id, stored),
    )
}

/** Done needs who and when: a `done` without them, or an unknown status, reads as open. */
private fun statusOf(stored: StoredItem): ItemStatus {
    val by = stored.completedBy
    val at = stored.completedAt
    return if (stored.status == STATUS_DONE && by != null && at != null) ItemStatus.Done(by, at) else ItemStatus.Open
}

private fun presetOf(id: String, preset: String): Preset? {
    val known = Preset.entries.firstOrNull { it.name.lowercase() == preset }
    if (known == null) Logger.w { "Item $id has the unknown preset $preset, read as none" }
    return known
}

private fun modulesOf(id: String, stored: StoredItem): ItemModules {
    val known = ModuleType.entries.associateBy(::moduleKey)
    (stored.moduleKeys - known.keys).forEach { Logger.w { "Item $id has the unknown module $it, skipped" } }
    val modules = stored.modules
    val read = ItemModules(
        schedule = modules.schedule?.let(::scheduleOf),
        assignment = modules.assignment?.let(::assignmentOf),
        checklist = modules.checklist?.let(::checklistOf),
        reminder = modules.reminder?.let(::reminderOf),
    )
    stored.moduleKeys.mapNotNull(known::get).filter { read[it] == null }.forEach {
        Logger.w { "Item $id has a malformed or unknown ${moduleKey(it)} module, skipped" }
    }
    return read
}

private fun scheduleOf(document: ScheduleDocument): Schedule? = try {
    Schedule(
        date = LocalDate.parse(document.date),
        time = document.time?.let(LocalTime::parse),
        endDate = document.endDate?.let(LocalDate::parse),
        duration = document.durationMinutes?.minutes,
    )
} catch (_: IllegalArgumentException) {
    null
}

private fun assignmentOf(document: AssignmentDocument): Assignment? = when (document.mode) {
    "member" -> document.assigneeId?.let(Assignment::ToMember)
    "anyone" -> Assignment.Anyone
    "claim" -> Assignment.Claim(document.assigneeId)
    else -> null
}

/** Entries without text are left by a tick on a removed entry, and skipped. */
private fun checklistOf(document: ChecklistDocument) = Checklist(
    document.entries.entries
        .sortedBy { it.value.order ?: Int.MAX_VALUE }
        .mapNotNull { (id, entry) ->
            entry.text?.let { ChecklistEntry(id, it, entry.checked ?: false, entry.quantity) }
        },
)

private fun reminderOf(document: ReminderDocument) = Reminder(document.offsetsMinutes.map { it.minutes })
