package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAssistChip
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitInset
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitModuleCard
import com.orbit.app.core.designsystem.component.OrbitPill
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_list
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.ic_person
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_add_detail
import orbit.shared.generated.resources.item_back
import orbit.shared.generated.resources.item_load_failed
import orbit.shared.generated.resources.item_load_failed_body
import orbit.shared.generated.resources.item_loading
import orbit.shared.generated.resources.item_not_found
import orbit.shared.generated.resources.item_not_found_body
import orbit.shared.generated.resources.item_type_event
import orbit.shared.generated.resources.item_type_list
import orbit.shared.generated.resources.item_type_other
import orbit.shared.generated.resources.item_type_task
import orbit.shared.generated.resources.item_unknown_member
import orbit.shared.generated.resources.module_add_assignment
import orbit.shared.generated.resources.module_add_checklist
import orbit.shared.generated.resources.module_add_reminder
import orbit.shared.generated.resources.module_add_schedule
import orbit.shared.generated.resources.module_assignment_anyone
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
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration

/** One item, composed from its modules, with chips for the details that can still be added. */
@Composable
fun ItemScreen(
    spaceId: String,
    itemId: String,
    onBack: () -> Unit,
    viewModel: ItemViewModel = koinViewModel { parametersOf(spaceId, itemId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val typeLabel = stringResource(
        when (state.preset) {
            Preset.TASK -> Res.string.item_type_task
            Preset.SHOPPING_LIST -> Res.string.item_type_list
            Preset.EVENT -> Res.string.item_type_event
            null -> Res.string.item_type_other
        },
    )
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = typeLabel,
                subtitle = state.spaceName,
                navigationIcon = {
                    OrbitIconButton(
                        icon = Res.drawable.ic_arrow_back,
                        contentDescription = stringResource(Res.string.item_back),
                        onClick = onBack,
                    )
                },
            )
        },
        scrollable = true,
    ) {
        when {
            state.loading -> OrbitLoadingState(stringResource(Res.string.item_loading), Modifier.fillMaxWidth())
            state.loadFailed -> OrbitErrorState(
                title = stringResource(Res.string.item_load_failed),
                body = stringResource(Res.string.item_load_failed_body),
                retryText = stringResource(Res.string.retry),
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxWidth(),
            )

            state.notFound -> OrbitEmptyState(
                title = stringResource(Res.string.item_not_found),
                body = stringResource(Res.string.item_not_found_body),
                modifier = Modifier.fillMaxWidth(),
            )

            else -> {
                ItemHeader(state, typeLabel)
                for (module in state.modules) {
                    RenderModuleCard(
                        module = module,
                        state = state,
                        onRemove = { viewModel.removeModule(module.type) },
                        onEdit = viewModel::editSchedule,
                    )
                }
                if (state.availableModules.isNotEmpty()) {
                    AddDetailSection(state.availableModules, state.canSubmit, viewModel::addModule)
                }
                if (state.failed) {
                    Text(
                        text = stringResource(Res.string.item_action_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    state.scheduleEditor?.let { editor -> ScheduleEditorDialog(editor, state, viewModel) }
}

@Composable
private fun ItemHeader(state: ItemUiState, typeLabel: String) {
    OrbitCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
    ) {
        OrbitPill(typeLabel)
        Text(state.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        state.visibleDescription?.let { description ->
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** [onEdit] is only offered on modules that can be edited so far: the schedule. */
@Composable
private fun RenderModuleCard(module: Module, state: ItemUiState, onRemove: () -> Unit, onEdit: () -> Unit) {
    val title = when (module.type) {
        ModuleType.SCHEDULE -> Res.string.module_schedule_title
        ModuleType.ASSIGNMENT -> Res.string.module_assignment_title
        ModuleType.CHECKLIST -> Res.string.module_checklist_title
        ModuleType.REMINDER -> Res.string.module_reminder_title
    }
    val icon = when (module.type) {
        ModuleType.SCHEDULE -> Res.drawable.ic_event
        ModuleType.ASSIGNMENT -> Res.drawable.ic_person
        ModuleType.CHECKLIST -> Res.drawable.ic_list
        ModuleType.REMINDER -> Res.drawable.ic_notifications
    }
    val removeDescription = when (module.type) {
        ModuleType.SCHEDULE -> Res.string.module_schedule_remove
        ModuleType.ASSIGNMENT -> Res.string.module_assignment_remove
        ModuleType.CHECKLIST -> Res.string.module_checklist_remove
        ModuleType.REMINDER -> Res.string.module_reminder_remove
    }
    OrbitModuleCard(
        title = stringResource(title),
        icon = icon,
        removeDescription = stringResource(removeDescription),
        onRemove = onRemove,
        removeEnabled = state.canSubmit,
        modifier = Modifier.fillMaxWidth(),
        editDescription = if (module is Schedule) stringResource(Res.string.module_schedule_edit) else null,
        onEdit = onEdit,
    ) {
        when (module) {
            is Schedule -> {
                val date = module.endDate?.let { end ->
                    stringResource(Res.string.module_schedule_range, itemDateLabel(module.date), itemDateLabel(end))
                } ?: itemDateLabel(module.date)
                val dateAndTime = module.time?.let { time ->
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
                module.duration?.let { duration ->
                    Text(
                        stringResource(Res.string.module_schedule_duration, duration.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            is Assignment -> {
                val assignee = when (module) {
                    Assignment.Anyone -> stringResource(Res.string.module_assignment_anyone)
                    is Assignment.Claim -> if (module.assigneeId == null) stringResource(Res.string.module_assignment_unassigned)
                    else state.assigneeName ?: stringResource(Res.string.item_unknown_member)

                    is Assignment.ToMember -> state.assigneeName ?: stringResource(Res.string.item_unknown_member)
                }
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
                    Text(assignee, style = MaterialTheme.typography.bodyLarge)
                }
            }

            is Checklist -> {
                if (module.entries.isEmpty()) {
                    Text(
                        stringResource(Res.string.module_checklist_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
                        for (entry in module.entries) {
                            OrbitInset(Modifier.fillMaxWidth()) {
                                // Rendering the module here; editing its entries belongs to the checklist flow.
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
            }

            is Reminder -> {
                val labels = module.offsets.map { offset ->
                    if (offset == Duration.ZERO) stringResource(Res.string.module_reminder_at_time)
                    else stringResource(Res.string.module_reminder_before, offset.toString())
                }
                Text(
                    text = labels.joinToString().ifEmpty { stringResource(Res.string.module_reminder_empty) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun AddDetailSection(available: List<ModuleType>, enabled: Boolean, onAdd: (ModuleType) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
    ) {
        Text(stringResource(Res.string.item_add_detail), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            for (type in available) {
                val label = when (type) {
                    ModuleType.SCHEDULE -> Res.string.module_add_schedule
                    ModuleType.ASSIGNMENT -> Res.string.module_add_assignment
                    ModuleType.CHECKLIST -> Res.string.module_add_checklist
                    ModuleType.REMINDER -> Res.string.module_add_reminder
                }
                OrbitAssistChip(
                    label = stringResource(label),
                    enabled = enabled,
                    onClick = { onAdd(type) },
                )
            }
        }
    }
}
