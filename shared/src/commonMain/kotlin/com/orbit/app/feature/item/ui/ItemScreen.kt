package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAssistChip
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitPill
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_add_detail
import orbit.shared.generated.resources.item_back
import orbit.shared.generated.resources.item_load_failed
import orbit.shared.generated.resources.item_load_failed_body
import orbit.shared.generated.resources.item_loading
import orbit.shared.generated.resources.item_not_found
import orbit.shared.generated.resources.item_not_found_body
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
                ItemHeader(state, typeLabel)
                for (module in state.modules) {
                    ItemModuleCard(
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
                    OrbitErrorText(stringResource(Res.string.item_action_failed))
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
