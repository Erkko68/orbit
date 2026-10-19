package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitInset
import com.orbit.app.core.designsystem.component.OrbitModuleCard
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.feature.item.ItemUiState
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_list
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.ic_person
import orbit.shared.generated.resources.module_add_assignment
import orbit.shared.generated.resources.module_add_checklist
import orbit.shared.generated.resources.module_add_reminder
import orbit.shared.generated.resources.module_add_schedule
import orbit.shared.generated.resources.module_assignment_remove
import orbit.shared.generated.resources.module_assignment_title
import orbit.shared.generated.resources.module_checklist_empty
import orbit.shared.generated.resources.module_checklist_quantity
import orbit.shared.generated.resources.module_checklist_remove
import orbit.shared.generated.resources.module_checklist_title
import orbit.shared.generated.resources.module_reminder_at_time
import orbit.shared.generated.resources.module_reminder_before
import orbit.shared.generated.resources.module_reminder_empty
import orbit.shared.generated.resources.module_reminder_remove
import orbit.shared.generated.resources.module_reminder_title
import orbit.shared.generated.resources.module_schedule_duration
import orbit.shared.generated.resources.module_schedule_edit
import orbit.shared.generated.resources.module_schedule_range
import orbit.shared.generated.resources.module_schedule_remove
import orbit.shared.generated.resources.module_schedule_time
import orbit.shared.generated.resources.module_schedule_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/** One module of the item on its card. Only the schedule can be edited so far, so only it offers [onEdit]. */
@Composable
internal fun ItemModuleCard(module: Module, state: ItemUiState, onRemove: () -> Unit, onEdit: () -> Unit) {
    OrbitModuleCard(
        title = stringResource(module.type.title),
        icon = module.type.icon,
        removeDescription = stringResource(module.type.removeDescription),
        onRemove = onRemove,
        removeEnabled = state.canSubmit,
        modifier = Modifier.fillMaxWidth(),
        editDescription = if (module is Schedule) stringResource(Res.string.module_schedule_edit) else null,
        onEdit = onEdit,
    ) {
        when (module) {
            is Schedule -> ScheduleContent(module)
            is Assignment -> AssignmentContent(state)
            is Checklist -> ChecklistContent(module)
            is Reminder -> ReminderContent(module)
        }
    }
}

/** What the "Add detail" chip of a module says. */
internal val ModuleType.addLabel: StringResource
    get() = when (this) {
        ModuleType.SCHEDULE -> Res.string.module_add_schedule
        ModuleType.ASSIGNMENT -> Res.string.module_add_assignment
        ModuleType.CHECKLIST -> Res.string.module_add_checklist
        ModuleType.REMINDER -> Res.string.module_add_reminder
    }

private val ModuleType.title: StringResource
    get() = when (this) {
        ModuleType.SCHEDULE -> Res.string.module_schedule_title
        ModuleType.ASSIGNMENT -> Res.string.module_assignment_title
        ModuleType.CHECKLIST -> Res.string.module_checklist_title
        ModuleType.REMINDER -> Res.string.module_reminder_title
    }

private val ModuleType.icon: DrawableResource
    get() = when (this) {
        ModuleType.SCHEDULE -> Res.drawable.ic_event
        ModuleType.ASSIGNMENT -> Res.drawable.ic_person
        ModuleType.CHECKLIST -> Res.drawable.ic_list
        ModuleType.REMINDER -> Res.drawable.ic_notifications
    }

private val ModuleType.removeDescription: StringResource
    get() = when (this) {
        ModuleType.SCHEDULE -> Res.string.module_schedule_remove
        ModuleType.ASSIGNMENT -> Res.string.module_assignment_remove
        ModuleType.CHECKLIST -> Res.string.module_checklist_remove
        ModuleType.REMINDER -> Res.string.module_reminder_remove
    }

@Composable
private fun ScheduleContent(schedule: Schedule) {
    val date = schedule.endDate?.let { end ->
        stringResource(Res.string.module_schedule_range, itemDateLabel(schedule.date), itemDateLabel(end))
    } ?: itemDateLabel(schedule.date)
    val dateAndTime = schedule.time?.let { time ->
        stringResource(Res.string.module_schedule_time, date, time.toString())
    } ?: date
    OrbitInset(Modifier.fillMaxWidth()) {
        Icon(
            painterResource(Res.drawable.ic_event),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(dateAndTime, style = MaterialTheme.typography.bodyLarge)
    }
    schedule.duration?.let { duration ->
        Text(
            stringResource(Res.string.module_schedule_duration, itemDurationLabel(duration)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AssignmentContent(state: ItemUiState) {
    OrbitInset(Modifier.fillMaxWidth()) {
        val initials = state.assigneeInitials
        if (initials != null) {
            OrbitAvatar(initials = initials, accent = MaterialTheme.colorScheme.primary)
        } else {
            Icon(
                painterResource(Res.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = state.assignmentNote?.let { stringResource(it) } ?: state.assigneeName.orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ChecklistContent(checklist: Checklist) {
    if (checklist.entries.isEmpty()) {
        Text(
            stringResource(Res.string.module_checklist_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        for (entry in checklist.entries) {
            OrbitInset(Modifier.fillMaxWidth()) {
                // Read-only until the checklist can be edited (#35).
                Checkbox(checked = entry.checked, onCheckedChange = null)
                val label = entry.quantity?.let { quantity ->
                    stringResource(Res.string.module_checklist_quantity, entry.text, quantity)
                } ?: entry.text
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (entry.checked) TextDecoration.LineThrough else TextDecoration.None,
                )
            }
        }
    }
}

@Composable
private fun ReminderContent(reminder: Reminder) {
    val labels = reminder.offsets.map { offset ->
        if (offset == Duration.ZERO) {
            stringResource(Res.string.module_reminder_at_time)
        } else {
            stringResource(Res.string.module_reminder_before, itemDurationLabel(offset))
        }
    }
    Text(
        text = labels.joinToString().ifEmpty { stringResource(Res.string.module_reminder_empty) },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
