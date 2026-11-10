package com.orbit.app.data.mapper

import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.SPACE_ACCENT_COUNT
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.SpaceProposal
import com.orbit.app.domain.model.validateItem
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlin.time.Duration.Companion.minutes

/** The strict default: a field the types do not know, such as an unknown module, makes the proposal unreadable. */
private val strict = Json

/**
 * The proposals in [plan] that the app can show (#65), each read on its own and checked with [validateItem], as
 * manual input is: one that is not is dropped and logged. [members] are the uids behind the handles (`m1` first). A
 * plan in a version this app does not read, or with a space that is not one, could be misread, so it is not used.
 */
internal fun planOf(plan: JsonObject, members: List<String>): Plan {
    val version = (plan["version"] as? JsonPrimitive)?.intOrNull
    val items = plan["items"] as? JsonArray
    if (version != PLAN_VERSION || items == null) throw AssistantException(AssistantError.UNAVAILABLE)
    val space = plan["space"]?.let { element ->
        decoded(SpaceProposalDocument.serializer(), element)
            ?.takeIf { it.name.isNotBlank() && it.accent in 0 until SPACE_ACCENT_COUNT }
            ?.let { SpaceProposal(it.name.trim(), it.accent) }
            ?: throw AssistantException(AssistantError.UNAVAILABLE)
    }
    return Plan(space, items.mapIndexedNotNull { index, element -> itemOf(index, element, members) })
}

private fun itemOf(index: Int, element: JsonElement, members: List<String>): NewItem? {
    val document = decoded(ItemProposalDocument.serializer(), element)
    val modules = document?.let { modulesOf(it.modules, members) }
    if (document == null || modules == null) {
        Logger.w { "Dropped proposal $index of the plan: not an item of version $PLAN_VERSION" }
        return null
    }
    val errors = validateItem(document.title, modules, members)
    if (errors.isNotEmpty()) {
        Logger.w { "Dropped proposal $index of the plan: $errors" }
        return null
    }
    return NewItem(document.title.trim(), document.description?.trim()?.ifEmpty { null }, modules)
}

private fun <T> decoded(deserializer: DeserializationStrategy<T>, element: JsonElement): T? = try {
    strict.decodeFromJsonElement(deserializer, element)
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}

/** The modules in the app's types, or null when one cannot be read as one: a date off the calendar, an unknown unit. */
private fun modulesOf(document: ProposedModulesDocument, members: List<String>): ItemModules? = try {
    ItemModules(
        schedule = document.schedule?.let { schedule ->
            Schedule(
                date = LocalDate.parse(schedule.date),
                time = schedule.time?.let(LocalTime::parse),
                endDate = schedule.endDate?.let(LocalDate::parse),
                duration = schedule.durationMinutes?.minutes,
            )
        },
        recurrence = document.recurrence?.let(::recurrenceOf),
        assignment = document.assignment?.let { assignmentOf(it, members) },
        checklist = document.checklist?.let { checklist ->
            require(checklist.entries.isNotEmpty())
            Checklist(
                checklist.entries.mapIndexed { order, entry ->
                    ChecklistEntry("entry-$order", entry.text.trim(), quantity = entry.quantity, order = order)
                },
            )
        },
        reminder = document.reminder?.let { Reminder(it.offsetsMinutes.map { offset -> offset.minutes }) },
    )
} catch (_: IllegalArgumentException) {
    null
}

private fun recurrenceOf(document: ProposedRecurrenceDocument): Recurrence {
    val until = document.until?.let(LocalDate::parse)
    return when (document.unit) {
        "day" -> Recurrence.Days(document.every, until)
        "week" -> Recurrence.Weeks(document.every, document.weekdays.orEmpty().map(::DayOfWeek).toSet(), until)
        "month" -> Recurrence.Months(document.every, document.dayOfMonth ?: 0, until)
        else -> throw IllegalArgumentException("Unknown recurrence unit ${document.unit}")
    }
}

/** A handle the request did not name stays as it is, so [validateItem] finds it is not a member. */
private fun assignmentOf(document: ProposedAssignmentDocument, members: List<String>): Assignment = when (document.mode) {
    "member" -> {
        require(document.assignees.isNotEmpty())
        Assignment.ToMembers(document.assignees.map { handle -> memberOf(handle, members) ?: handle }.toSet())
    }
    "anyone" -> Assignment.Anyone.also { require(document.assignees.isEmpty()) }
    else -> throw IllegalArgumentException("Unknown assignment mode ${document.mode}")
}

private fun memberOf(handle: String, members: List<String>): String? =
    handle.removePrefix("m").takeIf { handle.startsWith("m") }?.toIntOrNull()?.let { members.getOrNull(it - 1) }
