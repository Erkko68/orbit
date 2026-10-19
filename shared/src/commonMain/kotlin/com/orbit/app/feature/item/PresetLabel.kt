package com.orbit.app.feature.item

import com.orbit.app.domain.model.Preset
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_list
import orbit.shared.generated.resources.ic_task
import orbit.shared.generated.resources.item_type_event
import orbit.shared.generated.resources.item_type_list
import orbit.shared.generated.resources.item_type_other
import orbit.shared.generated.resources.item_type_task
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/** How every screen names an item's preset. An item without one is just an item. */
val Preset?.label: StringResource
    get() = when (this) {
        Preset.TASK -> Res.string.item_type_task
        Preset.SHOPPING_LIST -> Res.string.item_type_list
        Preset.EVENT -> Res.string.item_type_event
        null -> Res.string.item_type_other
    }

/** The icon of an item's preset. */
val Preset?.icon: DrawableResource
    get() = when (this) {
        Preset.TASK, null -> Res.drawable.ic_task
        Preset.SHOPPING_LIST -> Res.drawable.ic_list
        Preset.EVENT -> Res.drawable.ic_event
    }
