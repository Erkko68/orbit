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
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.isoDayNumber
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
    createdBy = creatorId,
    createdAt = Timestamp.ServerTimestamp,
    modules = ModulesDocument(
        schedule = item.modules.schedule?.let(::scheduleDocument),
        recurrence = item.modules.recurrence?.let(::recurrenceDocument),
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

internal fun recurrenceDocument(recurrence: Recurrence): RecurrenceDocument {
    val until = recurrence.until?.toString()
    return when (recurrence) {
        is Recurrence.Days -> RecurrenceDocument("day", recurrence.every, until = until)
        is Recurrence.Weeks -> RecurrenceDocument(
            "week",
            recurrence.every,
            weekdays = recurrence.weekdays.map { it.isoDayNumber }.sorted(),
            until = until,
        )
        is Recurrence.Months -> RecurrenceDocument("month", recurrence.every, dayOfMonth = recurrence.dayOfMonth, until = until)
    }
}

internal fun assignmentDocument(assignment: Assignment) = when (assignment) {
    is Assignment.ToMembers -> AssignmentDocument("member", assignment.assigneeIds.toList())
    Assignment.Anyone -> AssignmentDocument("anyone")
}

/** Entries keyed by id, their list position kept in `order`. */
internal fun checklistDocument(checklist: Checklist) = ChecklistDocument(
    checklist.entries.withIndex().associate { (order, entry) ->
        entry.id to EntryDocument(entry.text, entry.checked, entry.quantity, entry.order ?: order)
    },
)

internal fun reminderDocument(reminder: Reminder) =
    ReminderDocument(reminder.offsets.map { it.inWholeMinutes.toInt() })

/**
 * An item from its stored fields, or null when it has no title, creator or creation time: a malformed
 * document is skipped by the caller. What a newer build may have written (an unknown module or assignment
 * mode) is skipped and logged, so the rest of the item still shows.
 */
internal fun mapItem(id: String, stored: StoredItem): Item? {
    val title = stored.title ?: return null
    val createdBy = stored.createdBy ?: return null
    val createdAt = stored.createdAt ?: return null
    return Item(
        id = id,
        title = title,
        description = listOfNotNull(stored.description, stored.details).filter { it.isNotBlank() }
            .joinToString("\n\n").ifEmpty { null },
        status = statusOf(stored),
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

private fun modulesOf(id: String, stored: StoredItem): ItemModules {
    val known = ModuleType.entries.associateBy(::moduleKey)
    (stored.moduleKeys - known.keys).forEach { Logger.w { "Item $id has the unknown module $it, skipped" } }
    val modules = stored.modules
    val read = ItemModules(
        schedule = modules.schedule?.let(::scheduleOf),
        recurrence = modules.recurrence?.let(::recurrenceOf),
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

/** An unknown unit, or a rule that could never repeat, is skipped. */
private fun recurrenceOf(document: RecurrenceDocument): Recurrence? {
    if (document.every < 1) return null
    val until = try {
        document.until?.let(LocalDate::parse)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return when (document.unit) {
        "day" -> Recurrence.Days(document.every, until)
        "week" -> document.weekdays.orEmpty()
            .takeIf { days -> days.isNotEmpty() && days.all { it in 1..7 } }
            ?.let { days -> Recurrence.Weeks(document.every, days.map(::DayOfWeek).toSet(), until) }
        "month" -> document.dayOfMonth?.takeIf { it in 1..31 }
            ?.let { Recurrence.Months(document.every, it, until) }
        else -> null
    }
}

/** Older items have one `assigneeId`, and first to claim is gone: a claimed item is that member's, the rest anyone's. */
private fun assignmentOf(document: AssignmentDocument): Assignment? {
    val members = (document.assigneeIds ?: listOfNotNull(document.assigneeId)).toSet()
    return when (document.mode) {
        "member" -> Assignment.ToMembers(members).takeIf { members.isNotEmpty() }
        "anyone" -> Assignment.Anyone
        "claim" -> if (members.isEmpty()) Assignment.Anyone else Assignment.ToMembers(members)
        else -> null
    }
}

/** Entries without text are left by a tick on a removed entry, and skipped. */
private fun checklistOf(document: ChecklistDocument) = Checklist(
    document.entries.entries
        .sortedWith(compareBy({ it.value.order ?: Int.MAX_VALUE }, { it.key }))
        .mapNotNull { (id, entry) ->
            entry.text?.let { ChecklistEntry(id, it, entry.checked ?: false, entry.quantity, entry.order) }
        },
)

private fun reminderOf(document: ReminderDocument) = Reminder(document.offsetsMinutes.map { it.minutes })
