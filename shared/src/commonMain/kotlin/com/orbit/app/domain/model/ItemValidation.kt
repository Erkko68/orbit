package com.orbit.app.domain.model

import kotlin.time.Duration

const val ITEM_TITLE_MAX_LENGTH = 120

sealed interface ItemError {
    data object BlankTitle : ItemError
    data object TitleTooLong : ItemError
    data object TimeWithEndDate : ItemError
    data object EndNotAfterStart : ItemError
    data object NonPositiveDuration : ItemError
    data object RecurrenceWithoutSchedule : ItemError
    data object NonPositiveInterval : ItemError
    data object NoWeekdays : ItemError
    data object DayOfMonthOutOfRange : ItemError
    data object RecurrenceEndsBeforeStart : ItemError
    data object AssigneeNotMember : ItemError
    data class BlankChecklistEntry(val entryId: String) : ItemError
    data class NonPositiveQuantity(val entryId: String) : ItemError
    data object ReminderWithoutSchedule : ItemError
    data object NegativeReminderOffset : ItemError
}

/**
 * Every rule an item must pass before it is written, and that an assistant proposal is checked
 * against. Returns the errors instead of throwing so a form can show them.
 */
fun validateItem(
    title: String,
    modules: ItemModules,
    memberIds: Collection<String>,
): List<ItemError> = titleErrors(title) +
    scheduleErrors(modules.schedule) +
    recurrenceErrors(modules.recurrence, modules.schedule) +
    assignmentErrors(modules.assignment, memberIds) +
    checklistErrors(modules.checklist) +
    reminderErrors(modules.reminder, hasSchedule = modules.schedule != null)

private fun titleErrors(title: String): List<ItemError> {
    val trimmed = title.trim()
    return listOfNotNull(
        ItemError.BlankTitle.takeIf { trimmed.isEmpty() },
        ItemError.TitleTooLong.takeIf { trimmed.length > ITEM_TITLE_MAX_LENGTH },
    )
}

/** The rules of a schedule on its own, for an editor that checks it before the rest of the item. */
fun scheduleErrors(schedule: Schedule?): List<ItemError> {
    if (schedule == null) return emptyList()
    val endDate = schedule.endDate
    val duration = schedule.duration
    return listOfNotNull(
        ItemError.TimeWithEndDate.takeIf { endDate != null && schedule.time != null },
        ItemError.EndNotAfterStart.takeIf { endDate != null && endDate <= schedule.date },
        ItemError.NonPositiveDuration.takeIf { duration != null && duration <= Duration.ZERO },
    )
}

/** The rules of a recurrence over its [schedule], for the same editor. */
fun recurrenceErrors(recurrence: Recurrence?, schedule: Schedule?): List<ItemError> {
    if (recurrence == null) return emptyList()
    val until = recurrence.until
    return listOfNotNull(
        ItemError.RecurrenceWithoutSchedule.takeIf { schedule == null },
        ItemError.NonPositiveInterval.takeIf { recurrence.every < 1 },
        ItemError.NoWeekdays.takeIf { recurrence is Recurrence.Weeks && recurrence.weekdays.isEmpty() },
        ItemError.DayOfMonthOutOfRange.takeIf { recurrence is Recurrence.Months && recurrence.dayOfMonth !in 1..31 },
        ItemError.RecurrenceEndsBeforeStart.takeIf { until != null && schedule != null && until < schedule.date },
    )
}

private fun assignmentErrors(assignment: Assignment?, memberIds: Collection<String>): List<ItemError> {
    val outsiders = assignment?.assigneeIds.orEmpty() - memberIds.toSet()
    return listOfNotNull(ItemError.AssigneeNotMember.takeIf { outsiders.isNotEmpty() })
}

private fun checklistErrors(checklist: Checklist?): List<ItemError> =
    checklist?.entries.orEmpty().flatMap { entry ->
        listOfNotNull(
            ItemError.BlankChecklistEntry(entry.id).takeIf { entry.text.isBlank() },
            ItemError.NonPositiveQuantity(entry.id).takeIf { entry.quantity != null && entry.quantity <= 0 },
        )
    }

private fun reminderErrors(reminder: Reminder?, hasSchedule: Boolean): List<ItemError> {
    if (reminder == null) return emptyList()
    return listOfNotNull(
        ItemError.ReminderWithoutSchedule.takeIf { !hasSchedule },
        ItemError.NegativeReminderOffset.takeIf { reminder.offsets.any { it < Duration.ZERO } },
    )
}
