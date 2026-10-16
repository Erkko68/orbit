package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitBadge
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitCheckbox
import com.orbit.app.core.designsystem.component.OrbitDetailRow
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitModuleCard
import com.orbit.app.core.designsystem.component.OrbitProgressCard
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
import kotlin.time.Duration
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.ic_list
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.ic_person
import orbit.shared.generated.resources.ic_schedule
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_add_detail
import orbit.shared.generated.resources.item_back
import orbit.shared.generated.resources.item_checked_count
import orbit.shared.generated.resources.item_created
import orbit.shared.generated.resources.item_created_by
import orbit.shared.generated.resources.item_date
import orbit.shared.generated.resources.item_description_label
import orbit.shared.generated.resources.item_details
import orbit.shared.generated.resources.item_details_failed
import orbit.shared.generated.resources.item_entries
import orbit.shared.generated.resources.item_load_failed
import orbit.shared.generated.resources.item_load_failed_body
import orbit.shared.generated.resources.item_loading
import orbit.shared.generated.resources.item_mark_done
import orbit.shared.generated.resources.item_mark_open
import orbit.shared.generated.resources.item_not_found
import orbit.shared.generated.resources.item_not_found_body
import orbit.shared.generated.resources.item_space
import orbit.shared.generated.resources.item_status_done
import orbit.shared.generated.resources.item_tap_to_done
import orbit.shared.generated.resources.item_tap_to_open
import orbit.shared.generated.resources.item_time
import orbit.shared.generated.resources.item_today
import orbit.shared.generated.resources.item_type_event
import orbit.shared.generated.resources.item_type_list
import orbit.shared.generated.resources.item_type_other
import orbit.shared.generated.resources.item_type_task
import orbit.shared.generated.resources.item_unknown_member
import orbit.shared.generated.resources.item_when
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
import orbit.shared.generated.resources.module_schedule_range
import orbit.shared.generated.resources.module_schedule_remove
import orbit.shared.generated.resources.module_schedule_time
import orbit.shared.generated.resources.module_schedule_title
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
                ItemHeader(state, typeLabel, viewModel::toggleStatus)
                if (state.showChecklistProgress) {
                    OrbitProgressCard(
                        label = stringResource(Res.string.item_checked_count, state.checkedCount, state.entryCount),
                        progress = state.checklistProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.eventSchedule?.let { schedule ->
                    RenderModuleCard(schedule, state, onRemove = { viewModel.removeModule(schedule.type) })
                }
                state.visibleDescription?.let { description ->
                    OrbitCard(Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(Res.string.item_description_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                for (module in state.modules) {
                    if (module == state.eventSchedule) continue
                    RenderModuleCard(
                        module = module,
                        state = state,
                        onRemove = { viewModel.removeModule(module.type) },
                    )
                }
                ItemDetails(state)
                if (state.availableModules.isNotEmpty()) {
                    AddDetailSection(state.availableModules, state.canMutate, viewModel::addModule)
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
}

@Composable
private fun ItemHeader(state: ItemUiState, typeLabel: String, onToggleStatus: () -> Unit) {
    OrbitCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.showCompletion) {
                val actionLabel = stringResource(
                    if (state.isDone) Res.string.item_mark_open else Res.string.item_mark_done,
                )
                OrbitCheckbox(
                    checked = state.isDone,
                    onCheckedChange = { onToggleStatus() },
                    enabled = state.canMutate,
                    modifier = Modifier.semantics { contentDescription = actionLabel },
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (state.isDone) TextDecoration.LineThrough else TextDecoration.None,
                )
                if (state.showCompletion) {
                    Text(
                        text = stringResource(
                            if (state.isDone) Res.string.item_tap_to_open else Res.string.item_tap_to_done,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            OrbitBadge(typeLabel)
            if (state.isDone) OrbitBadge(stringResource(Res.string.item_status_done))
        }
    }
}

@Composable
private fun RenderModuleCard(module: Module, state: ItemUiState, onRemove: () -> Unit) {
    val title = when (module.type) {
        ModuleType.SCHEDULE -> if (state.eventSchedule != null) Res.string.item_when else Res.string.module_schedule_title
        ModuleType.ASSIGNMENT -> Res.string.module_assignment_title
        ModuleType.CHECKLIST -> if (state.preset == Preset.SHOPPING_LIST) Res.string.item_entries else Res.string.module_checklist_title
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
        removeEnabled = state.canMutate,
        highlighted = module == state.eventSchedule,
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (module) {
            is Schedule -> {
                val date = module.endDate?.let { end ->
                    stringResource(Res.string.module_schedule_range, itemDateLabel(module.date), itemDateLabel(end))
                } ?: itemDateLabel(module.date)
                val headlineDate = if (state.eventIsToday) stringResource(Res.string.item_today) else date
                val dateAndTime = module.time?.let { time ->
                    stringResource(Res.string.module_schedule_time, headlineDate, time.toString())
                } ?: headlineDate
                Text(
                    dateAndTime,
                    style = if (state.eventSchedule != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.eventIsToday) {
                    Text(
                        date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    else state.memberNames[module.assigneeId] ?: stringResource(Res.string.item_unknown_member)

                    is Assignment.ToMember -> state.memberNames[module.assigneeId]
                        ?: stringResource(Res.string.item_unknown_member)
                }
                Text(assignee, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            is Checklist -> {
                if (module.entries.isEmpty()) {
                    Text(
                        stringResource(Res.string.module_checklist_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column {
                        for (entry in module.entries) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Rendering the module here; editing its entries belongs to the checklist flow.
                                OrbitCheckbox(checked = entry.checked, onCheckedChange = null)
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
private fun ItemDetails(state: ItemUiState) {
    if (state.createdDate == null) return
    OrbitCard(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.item_details), style = MaterialTheme.typography.titleMedium)
        state.eventSchedule?.let { schedule ->
            val date = schedule.endDate?.let { end ->
                stringResource(Res.string.module_schedule_range, itemDateLabel(schedule.date), itemDateLabel(end))
            } ?: itemDateLabel(schedule.date)
            OrbitDetailRow(Res.drawable.ic_event, stringResource(Res.string.item_date), date, inlineValue = true)
            schedule.time?.let { time ->
                OrbitDetailRow(
                    Res.drawable.ic_schedule,
                    stringResource(Res.string.item_time),
                    time.toString(),
                    inlineValue = true
                )
            }
        }
        OrbitDetailRow(
            Res.drawable.ic_person,
            stringResource(Res.string.item_created_by),
            state.creatorName ?: stringResource(Res.string.item_unknown_member),
            inlineValue = state.horizontalDetails,
        )
        OrbitDetailRow(
            Res.drawable.ic_event,
            stringResource(Res.string.item_created),
            itemDateLabel(state.createdDate),
            inlineValue = state.horizontalDetails
        )
        state.spaceName?.let { name ->
            OrbitDetailRow(
                Res.drawable.ic_home,
                stringResource(Res.string.item_space),
                name,
                inlineValue = state.horizontalDetails
            )
        }
        if (state.detailsFailed) {
            Text(stringResource(Res.string.item_details_failed), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                OrbitFilterChip(
                    label = stringResource(label),
                    selected = false,
                    enabled = enabled,
                    onClick = { onAdd(type) },
                )
            }
        }
    }
}
