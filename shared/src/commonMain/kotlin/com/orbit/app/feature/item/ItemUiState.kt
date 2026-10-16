package com.orbit.app.feature.item

import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Schedule
import kotlinx.datetime.LocalDate

/**
 * Immutable state of the Item detail screen: the item loaded from the repository, its active
 * modules rendered as cards, and the modules not yet present available to add via chips.
 */
data class ItemUiState(
    val itemId: String = "",
    val spaceId: String = "",
    val title: String = "",
    val description: String? = null,
    val preset: Preset? = null,
    val status: ItemStatus = ItemStatus.Open,
    val modules: List<Module> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val notFound: Boolean = false,
    val failed: Boolean = false,
    val mutating: Boolean = false,
    val spaceName: String? = null,
    val creatorName: String? = null,
    val createdDate: LocalDate? = null,
    val memberNames: Map<String, String> = emptyMap(),
    val detailsFailed: Boolean = false,
    val today: LocalDate? = null,
) {
    val isDone: Boolean get() = status is ItemStatus.Done
    val canMutate: Boolean get() = !loading && !loadFailed && !notFound && !mutating
    val showCompletion: Boolean get() = preset == Preset.TASK || preset == null
    val visibleDescription: String? get() = description?.takeIf { it.isNotBlank() }
    val checklist: Checklist? get() = modules.filterIsInstance<Checklist>().firstOrNull()
    val checkedCount: Int get() = checklist?.entries?.count { it.checked } ?: 0
    val entryCount: Int get() = checklist?.entries?.size ?: 0
    val checklistProgress: Float get() = if (entryCount == 0) 0f else checkedCount.toFloat() / entryCount
    val showChecklistProgress: Boolean get() = preset == Preset.SHOPPING_LIST && checklist != null
    val eventSchedule: Schedule?
        get() = modules.filterIsInstance<Schedule>().firstOrNull()?.takeIf { preset == Preset.EVENT }
    val eventIsToday: Boolean
        get() = eventSchedule?.let { it.date == today && (it.endDate == null || it.endDate == it.date) } == true
    val horizontalDetails: Boolean get() = preset == Preset.EVENT

    /** Reminder needs a schedule; an existing module is never offered twice. */
    val availableModules: List<ModuleType>
        get() {
            if (loading || loadFailed || notFound) return emptyList()
            val present = modules.map { it.type }.toSet()
            return ModuleType.entries.filter { type ->
                type !in present && (type != ModuleType.REMINDER || ModuleType.SCHEDULE in present)
            }
        }
}
