package com.orbit.app.feature.spacehome.items

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitTaskRow
import com.orbit.app.feature.item.ui.face
import com.orbit.app.feature.item.ui.itemDueLabel
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.item_complete_named
import orbit.shared.generated.resources.item_reopen_named
import orbit.shared.generated.resources.module_schedule_time
import orbit.shared.generated.resources.space_item_entry_count
import org.jetbrains.compose.resources.stringResource

/**
 * An item as a row of the Tasks list, a task or a list alike: what, the first line of its description, when it is due,
 * how far along a list is and who has it. The check completes or reopens it, except on a list, which it opens instead:
 * a list finishes by ticking its entries (ADR 0008).
 */
@Composable
internal fun SpaceItemRow(
    item: SpaceItem,
    today: LocalDate?,
    onClick: () -> Unit,
    onDoneChange: (Boolean) -> Unit,
    submitting: Boolean,
    modifier: Modifier = Modifier,
) {
    OrbitTaskRow(
        title = item.title,
        checked = item.done,
        onCheckedChange = { done -> if (item.checklist == null) onDoneChange(done) else onClick() },
        checkDescription = stringResource(
            if (item.done) Res.string.item_reopen_named else Res.string.item_complete_named,
            item.title,
        ),
        onClick = onClick,
        modifier = modifier,
        summary = item.summary,
        label = spaceItemLabel(item, today)?.uppercase(),
        faces = item.faces.map { it.face() },
        enabled = !submitting,
        overdue = item.isOverdue(today),
    )
}

/** When the item is due, with the time if it has one, and how far along a list is. */
@Composable
internal fun spaceItemLabel(item: SpaceItem, today: LocalDate?): String? {
    val due = item.dueDate?.let { date ->
        val day = itemDueLabel(date, today)
        item.dueTime?.let { stringResource(Res.string.module_schedule_time, day, it.toString()) } ?: day
    }
    val count = item.checklist?.let { stringResource(Res.string.space_item_entry_count, it.done, it.total) }
    return listOfNotNull(due, count).joinToString(" · ").ifEmpty { null }
}
