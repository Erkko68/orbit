package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

enum class ModuleType { SCHEDULE, ASSIGNMENT, CHECKLIST, REMINDER }

sealed interface Module {
    val type: ModuleType
}

/** At most one module of each type, mirroring the stored `modules` map. */
data class ItemModules(
    val schedule: Schedule? = null,
    val assignment: Assignment? = null,
    val checklist: Checklist? = null,
    val reminder: Reminder? = null,
) {
    operator fun get(type: ModuleType): Module? = when (type) {
        ModuleType.SCHEDULE -> schedule
        ModuleType.ASSIGNMENT -> assignment
        ModuleType.CHECKLIST -> checklist
        ModuleType.REMINDER -> reminder
    }

    /** Present modules in card order. */
    val all: List<Module> get() = ModuleType.entries.mapNotNull(::get)

    fun with(module: Module): ItemModules = when (module) {
        is Schedule -> copy(schedule = module)
        is Assignment -> copy(assignment = module)
        is Checklist -> copy(checklist = module)
        is Reminder -> copy(reminder = module)
    }

    fun without(type: ModuleType): ItemModules = when (type) {
        ModuleType.SCHEDULE -> copy(schedule = null)
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
}

sealed interface Assignment : Module {
    override val type get() = ModuleType.ASSIGNMENT

    /** Who is responsible right now, whatever the mode. Backs the "mine" filter. */
    val assigneeId: String?

    data class ToMember(override val assigneeId: String) : Assignment

    data object Anyone : Assignment {
        override val assigneeId: String? get() = null
    }

    /** Unassigned until a member claims it. */
    data class Claim(override val assigneeId: String? = null) : Assignment
}

data class Checklist(val entries: List<ChecklistEntry>) : Module {
    override val type get() = ModuleType.CHECKLIST
}

data class ChecklistEntry(
    val id: String,
    val text: String,
    val checked: Boolean = false,
    val quantity: Int? = null,
)

/** Offsets before the schedule's due moment. */
data class Reminder(val offsets: List<Duration>) : Module {
    override val type get() = ModuleType.REMINDER
}
