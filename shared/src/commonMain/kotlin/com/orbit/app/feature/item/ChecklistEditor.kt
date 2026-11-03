package com.orbit.app.feature.item

import com.orbit.app.domain.model.ChecklistEntry

/** An entry of the checklist being edited in its sheet. */
data class ChecklistEditor(
    val entryId: String,
    val text: String = "",
    val quantity: String = "",
) {
    val quantityValue: Int? get() = quantity.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
    val quantityInvalid: Boolean get() = quantity.isNotBlank() && (quantityValue?.let { it <= 0 } ?: true)
    val canSave: Boolean get() = text.isNotBlank() && !quantityInvalid

    companion object {
        fun of(entry: ChecklistEntry) = ChecklistEditor(entry.id, entry.text, entry.quantity?.toString().orEmpty())
    }
}
