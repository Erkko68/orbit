package com.orbit.app.feature.item

import com.orbit.app.domain.model.ChecklistEntry

/** Text typed in the add/edit dialog. A null id means a new entry. */
data class ChecklistEditor(
    val entryId: String? = null,
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
