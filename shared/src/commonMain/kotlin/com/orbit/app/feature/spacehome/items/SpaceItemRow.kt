package com.orbit.app.feature.spacehome.items

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitTaskCard
import com.orbit.app.feature.item.ui.floating
import com.orbit.app.feature.item.ui.itemDueLabel
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.item_complete_named
import orbit.shared.generated.resources.item_reopen_named
import orbit.shared.generated.resources.module_schedule_time
import orbit.shared.generated.resources.space_item_entry_count
import org.jetbrains.compose.resources.stringResource

/** An item as a card: what, its summary, when it is due and who has it, and the check that completes or reopens it, unless it is a list. */
@Composable
internal fun SpaceItemRow(
    item: SpaceItem,
    today: LocalDate?,
    onClick: () -> Unit,
    onDoneChange: (Boolean) -> Unit,
    submitting: Boolean,
    modifier: Modifier = Modifier,
) {
    OrbitTaskCard(
        title = item.title,
        checked = item.done,
        // A list is finished by ticking its entries, so its card has no check of its own.
        onCheckedChange = onDoneChange.takeIf { item.checklist == null },
        checkDescription = stringResource(
            if (item.done) Res.string.item_reopen_named else Res.string.item_complete_named,
            item.title,
        ),
        onClick = onClick,
        modifier = modifier,
        summary = item.summary,
        label = spaceItemLabel(item, today)?.uppercase(),
        faces = item.faces.map { it.floating() },
        enabled = !submitting,
    )
}

/** When the item is due, with the time if it has one. A list that is done has no date to speak of, so its count stands in. */
@Composable
internal fun spaceItemLabel(item: SpaceItem, today: LocalDate?): String? {
    val due = item.dueDate?.let { itemDueLabel(it, today) }
    return when {
        due != null && item.dueTime != null -> stringResource(Res.string.module_schedule_time, due, item.dueTime.toString())
        due != null -> due
        else -> item.checklist?.takeIf { it.total > 0 }?.let {
            stringResource(Res.string.space_item_entry_count, it.done, it.total)
        }
    }
}
