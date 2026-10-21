package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitInset
import com.orbit.app.core.designsystem.component.OrbitModuleCard
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.feature.item.AssignmentMode
import com.orbit.app.feature.item.ItemUiState
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.assignment_change_member
import orbit.shared.generated.resources.assignment_claim
import orbit.shared.generated.resources.assignment_mode_anyone
import orbit.shared.generated.resources.assignment_mode_claim
import orbit.shared.generated.resources.assignment_mode_one_member
import orbit.shared.generated.resources.assignment_pick_member
import orbit.shared.generated.resources.assignment_unclaim
import orbit.shared.generated.resources.checklist_add_entry
import orbit.shared.generated.resources.checklist_check_entry
import orbit.shared.generated.resources.checklist_edit_entry
import orbit.shared.generated.resources.checklist_uncheck_entry
import orbit.shared.generated.resources.ic_chevron_right
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_edit
import orbit.shared.generated.resources.ic_list
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.ic_person
import orbit.shared.generated.resources.module_add_assignment
import orbit.shared.generated.resources.module_add_checklist
import orbit.shared.generated.resources.module_add_reminder
import orbit.shared.generated.resources.module_add_schedule
import orbit.shared.generated.resources.module_assignment_remove
import orbit.shared.generated.resources.module_assignment_title
import orbit.shared.generated.resources.module_assignment_unassigned
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

/** One module of the item on its card. Offers actions for editing or interacting with the module. */
@Composable
internal fun ItemModuleCard(
    module: Module,
    state: ItemUiState,
    onRemove: () -> Unit,
    onEdit: () -> Unit = {},
    onAssignToAnyone: () -> Unit = {},
    onAssignToClaim: () -> Unit = {},
    onOpenMemberPicker: () -> Unit = {},
    onClaim: () -> Unit = {},
    onUnclaim: () -> Unit = {},
    onAddEntry: () -> Unit = {},
    onEditEntry: (String) -> Unit = {},
    onToggleEntry: (String) -> Unit = {},
) {
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
            is Assignment -> AssignmentContent(
                state = state,
                onAssignToAnyone = onAssignToAnyone,
                onAssignToClaim = onAssignToClaim,
                onOpenMemberPicker = onOpenMemberPicker,
                onClaim = onClaim,
                onUnclaim = onUnclaim,
            )
            is Checklist -> ChecklistContent(module, state.canSubmit, onAddEntry, onEditEntry, onToggleEntry)
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
private fun AssignmentContent(
    state: ItemUiState,
    onAssignToAnyone: () -> Unit,
    onAssignToClaim: () -> Unit,
    onOpenMemberPicker: () -> Unit,
    onClaim: () -> Unit,
    onUnclaim: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            OrbitFilterChip(
                label = stringResource(Res.string.assignment_mode_anyone),
                selected = state.assignmentMode == AssignmentMode.ANYONE,
                enabled = state.canSubmit,
                onClick = { if (state.assignmentMode != AssignmentMode.ANYONE) onAssignToAnyone() },
            )
            OrbitFilterChip(
                label = stringResource(Res.string.assignment_mode_claim),
                selected = state.assignmentMode == AssignmentMode.CLAIM,
                enabled = state.canSubmit,
                onClick = { if (state.assignmentMode != AssignmentMode.CLAIM) onAssignToClaim() },
            )
            OrbitFilterChip(
                label = stringResource(Res.string.assignment_mode_one_member),
                selected = state.assignmentMode == AssignmentMode.MEMBER,
                enabled = state.canSubmit,
                onClick = onOpenMemberPicker,
            )
        }
        when (state.assignmentMode) {
            AssignmentMode.ANYONE, null -> Unit
            AssignmentMode.CLAIM -> {
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
                        text = state.assigneeName ?: stringResource(Res.string.module_assignment_unassigned),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (state.canClaim) {
                    OrbitButton(
                        text = stringResource(Res.string.assignment_claim),
                        onClick = onClaim,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSubmit,
                    )
                } else if (state.canUnclaim) {
                    OrbitOutlinedButton(
                        text = stringResource(Res.string.assignment_unclaim),
                        onClick = onUnclaim,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSubmit,
                    )
                }
            }
            AssignmentMode.MEMBER -> {
                OrbitInset(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = if (state.canSubmit) onOpenMemberPicker else null,
                ) {
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
                    val label = state.assigneeName
                        ?: state.assignmentNote?.let { stringResource(it) }
                        ?: stringResource(Res.string.assignment_pick_member)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painterResource(Res.drawable.ic_chevron_right),
                        contentDescription = stringResource(Res.string.assignment_change_member),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistContent(
    checklist: Checklist,
    enabled: Boolean,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onToggle: (String) -> Unit,
) {
    if (checklist.entries.isEmpty()) {
        Text(
            stringResource(Res.string.module_checklist_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            for (entry in checklist.entries) {
                OrbitInset(Modifier.fillMaxWidth()) {
                    val toggleDescription = stringResource(
                        if (entry.checked) Res.string.checklist_uncheck_entry else Res.string.checklist_check_entry,
                        entry.text,
                    )
                    Checkbox(
                        checked = entry.checked,
                        onCheckedChange = { onToggle(entry.id) },
                        enabled = enabled,
                        modifier = Modifier.semantics { contentDescription = toggleDescription },
                    )
                    val label = entry.quantity?.let { quantity ->
                        stringResource(Res.string.module_checklist_quantity, entry.text, quantity)
                    } ?: entry.text
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (entry.checked) TextDecoration.LineThrough else TextDecoration.None,
                    )
                    OrbitIconButton(
                        icon = Res.drawable.ic_edit,
                        contentDescription = stringResource(Res.string.checklist_edit_entry, entry.text),
                        onClick = { onEdit(entry.id) },
                        enabled = enabled,
                    )
                }
            }
        }
    }
    OrbitTextButton(stringResource(Res.string.checklist_add_entry), onAdd, enabled = enabled)
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
