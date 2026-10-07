package com.orbit.app.domain.model

import kotlin.time.Duration

const val ITEM_TITLE_MAX_LENGTH = 120

sealed interface ItemError {
    data object BlankTitle : ItemError
    data object TitleTooLong : ItemError
    data object TimeWithEndDate : ItemError
    data object EndNotAfterStart : ItemError
    data object NonPositiveDuration : ItemError
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
): List<ItemError> = buildList {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) add(ItemError.BlankTitle)
    if (trimmed.length > ITEM_TITLE_MAX_LENGTH) add(ItemError.TitleTooLong)

    modules.schedule?.let { schedule ->
        if (schedule.endDate != null) {
            if (schedule.time != null) add(ItemError.TimeWithEndDate)
            if (schedule.endDate <= schedule.date) add(ItemError.EndNotAfterStart)
        }
        if (schedule.duration != null && schedule.duration <= Duration.ZERO) {
            add(ItemError.NonPositiveDuration)
        }
    }

    modules.assignment?.assigneeId?.let { assigneeId ->
        if (assigneeId !in memberIds) add(ItemError.AssigneeNotMember)
    }

    modules.checklist?.entries?.forEach { entry ->
        if (entry.text.isBlank()) add(ItemError.BlankChecklistEntry(entry.id))
        if (entry.quantity != null && entry.quantity <= 0) add(ItemError.NonPositiveQuantity(entry.id))
    }

    modules.reminder?.let { reminder ->
        if (modules.schedule == null) add(ItemError.ReminderWithoutSchedule)
        if (reminder.offsets.any { it < Duration.ZERO }) add(ItemError.NegativeReminderOffset)
    }
}
