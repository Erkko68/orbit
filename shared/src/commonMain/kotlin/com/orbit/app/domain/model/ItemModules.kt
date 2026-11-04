package com.orbit.app.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Instant

enum class ModuleType { SCHEDULE, RECURRENCE, ASSIGNMENT, CHECKLIST, REMINDER }

sealed interface Module {
    val type: ModuleType
}

/** At most one module of each type, mirroring the stored `modules` map. */
data class ItemModules(
    val schedule: Schedule? = null,
    val recurrence: Recurrence? = null,
    val assignment: Assignment? = null,
    val checklist: Checklist? = null,
    val reminder: Reminder? = null,
) {
    operator fun get(type: ModuleType): Module? = when (type) {
        ModuleType.SCHEDULE -> schedule
        ModuleType.RECURRENCE -> recurrence
        ModuleType.ASSIGNMENT -> assignment
        ModuleType.CHECKLIST -> checklist
        ModuleType.REMINDER -> reminder
    }

    /** Present modules in card order. */
    val all: List<Module> get() = ModuleType.entries.mapNotNull(::get)

    /** The item is [userId]'s by name, not just because it is anyone's. Backs the "mine" filter. */
    fun isAssignedTo(userId: String): Boolean = assignment?.assigneeIds.orEmpty().contains(userId)

    /** A checklist with something on it: a list, which finishes by ticking its entries (ADR 0008). */
    val hasEntries: Boolean get() = checklist?.entries.orEmpty().isNotEmpty()

    fun with(module: Module): ItemModules = when (module) {
        is Schedule -> copy(schedule = module)
        is Recurrence -> copy(recurrence = module)
        is Assignment -> copy(assignment = module)
        is Checklist -> copy(checklist = module)
        is Reminder -> copy(reminder = module)
    }

    fun without(type: ModuleType): ItemModules = when (type) {
        ModuleType.SCHEDULE -> copy(schedule = null)
        ModuleType.RECURRENCE -> copy(recurrence = null)
        ModuleType.ASSIGNMENT -> copy(assignment = null)
        ModuleType.CHECKLIST -> copy(checklist = null)
        ModuleType.REMINDER -> copy(reminder = null)
    }
}

/** Due at a point (`date`, optionally `time`) or within a window (`date` to `endDate`). */
data class Schedule(
    val date: LocalDate,
    val time: LocalTime? = null,
    val endDate: LocalDate? = null,
    val duration: Duration? = null,
) : Module {
    override val type get() = ModuleType.SCHEDULE

    /** An open item is overdue once this is before today. */
    val deadline: LocalDate get() = endDate ?: date

    /** What reminders count back from: the deadline at [time], or at 09:00 without one, in the phone's [zone]. */
    fun dueMoment(zone: TimeZone): Instant = deadline.atTime(time ?: LocalTime(9, 0)).toInstant(zone)
}

/**
 * Repeats the schedule: its date is the current occurrence, and completing the item moves it to the next one
 * (ADR 0011). [until] is the last day an occurrence may fall on. Needs a schedule.
 */
sealed interface Recurrence : Module {
    override val type get() = ModuleType.RECURRENCE

    /** Every how many days, weeks or months: 1 or more. */
    val every: Int
    val until: LocalDate?

    data class Days(override val every: Int, override val until: LocalDate? = null) : Recurrence

    /** On [weekdays] of every [every]th week, counted from the week of the schedule's date (weeks start on Monday). */
    data class Weeks(
        override val every: Int,
        val weekdays: Set<DayOfWeek>,
        override val until: LocalDate? = null,
    ) : Recurrence

    /** On [dayOfMonth] (1 to 31), or the month's last day when it is shorter. */
    data class Months(override val every: Int, val dayOfMonth: Int, override val until: LocalDate? = null) : Recurrence
}

sealed interface Assignment : Module {
    override val type get() = ModuleType.ASSIGNMENT

    /** The members who have it by name: nobody in particular when it is anyone's. */
    val assigneeIds: Set<String>

    /** One or more members. Whoever of them completes it completes it for all of them. */
    data class ToMembers(override val assigneeIds: Set<String>) : Assignment

    data object Anyone : Assignment {
        override val assigneeIds: Set<String> get() = emptySet()
    }
}

data class Checklist(val entries: List<ChecklistEntry>) : Module {
    override val type get() = ModuleType.CHECKLIST
}

data class ChecklistEntry(
    val id: String,
    val text: String,
    val checked: Boolean = false,
    val quantity: Int? = null,
    val order: Int? = null,
)

/** Offsets before the schedule's due moment. */
data class Reminder(val offsets: List<Duration>) : Module {
    override val type get() = ModuleType.REMINDER
}
