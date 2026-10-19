package com.orbit.app.feature.item

import com.orbit.app.core.initialsOf
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.Preset

/**
 * Immutable state of the Item detail screen: the item loaded from the repository, its active
 * modules rendered as cards, and the modules not yet present available to add via chips.
 */
data class ItemUiState(
    val title: String = "",
    val description: String? = null,
    val preset: Preset? = null,
    val modules: List<Module> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val notFound: Boolean = false,
    val failed: Boolean = false,
    val submitting: Boolean = false,
    val spaceName: String? = null,
    val memberNames: Map<String, String> = emptyMap(),
    /** The schedule being edited, while its dialog is open. */
    val scheduleEditor: ScheduleEditor? = null,
) {
    val canSubmit: Boolean get() = !loading && !loadFailed && !notFound && !submitting
    val canSaveSchedule: Boolean get() = canSubmit && scheduleEditor?.schedule != null
    val visibleDescription: String? get() = description?.takeIf { it.isNotBlank() }

    /** The current assignee's public name, when the member context is available. */
    val assigneeName: String?
        get() = modules.filterIsInstance<Assignment>().firstOrNull()?.assigneeId
            ?.let { memberNames[it] }?.takeIf { it.isNotBlank() }
    val assigneeInitials: String? get() = assigneeName?.let(::initialsOf)

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
