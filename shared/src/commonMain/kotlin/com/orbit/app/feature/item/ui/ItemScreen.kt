package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
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
import com.orbit.app.core.designsystem.component.OrbitAssistChip
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitPill
import com.orbit.app.core.designsystem.component.OrbitPillTone
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.notification.permission.RequireNotificationPermission
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_add_detail
import orbit.shared.generated.resources.item_back
import orbit.shared.generated.resources.item_complete
import orbit.shared.generated.resources.item_load_failed
import orbit.shared.generated.resources.item_load_failed_body
import orbit.shared.generated.resources.item_loading
import orbit.shared.generated.resources.item_not_found
import orbit.shared.generated.resources.item_not_found_body
import orbit.shared.generated.resources.item_reopen
import orbit.shared.generated.resources.item_status_done
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
    val typeLabel = stringResource(state.typeLabel)
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
                ItemHeader(state, typeLabel, viewModel::toggleDone)
                for (module in state.modules) {
                    ItemModuleCard(
                        module = module,
                        state = state,
                        onRemove = { viewModel.removeModule(module.type) },
                        onEdit = viewModel::editSchedule,
                        onAssignToAnyone = viewModel::assignToAnyone,
                        onAssignToClaim = viewModel::assignToClaim,
                        onOpenMemberPicker = viewModel::openMemberPicker,
                        onClaim = viewModel::claim,
                        onUnclaim = viewModel::unclaim,
                        onAddEntry = viewModel::addChecklistEntry,
                        onEditEntry = viewModel::editChecklistEntry,
                        onToggleEntry = viewModel::toggleChecklistEntry,
                    )
                }
                if (state.availableModules.isNotEmpty()) {
                    AddDetailSection(state.availableModules, state.canSubmit, viewModel::addModule)
                }
                if (state.failed) {
                    OrbitErrorText(stringResource(Res.string.item_action_failed))
                }
            }
        }
    }
    state.scheduleEditor?.let { editor -> ScheduleEditorDialog(editor, state, viewModel) }
    state.checklistEditor?.let { editor -> ChecklistEditorDialog(editor, state, viewModel) }
    if (state.askNotifications) {
        RequireNotificationPermission(onResult = { viewModel.onNotificationsAnswered() })
    }
    if (state.pickingMember) {
        MemberPickerDialog(state, viewModel)
    }
}

@Composable
private fun ItemHeader(state: ItemUiState, typeLabel: String, onToggleDone: (Boolean) -> Unit) {
    OrbitCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitPill(typeLabel)
            if (state.isDone) {
                OrbitPill(
                    label = stringResource(Res.string.item_status_done),
                    tone = OrbitPillTone.Success,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            val completeDescription = stringResource(if (state.isDone) Res.string.item_reopen else Res.string.item_complete)
            Checkbox(
                checked = state.isDone,
                onCheckedChange = onToggleDone,
                enabled = state.canSubmit,
                modifier = Modifier.semantics { contentDescription = completeDescription },
            )
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = if (state.isDone) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
        state.visibleDescription?.let { description ->
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                OrbitAssistChip(
                    label = stringResource(type.addLabel),
                    enabled = enabled,
                    onClick = { onAdd(type) },
                )
            }
        }
    }
}
