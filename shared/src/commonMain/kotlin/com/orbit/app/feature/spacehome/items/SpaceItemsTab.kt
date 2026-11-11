package com.orbit.app.feature.spacehome.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitTurnCard
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.item.ui.itemDueLabel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.hide
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.show
import orbit.shared.generated.resources.space_items_done
import orbit.shared.generated.resources.space_items_empty_body
import orbit.shared.generated.resources.space_items_empty_title
import orbit.shared.generated.resources.space_items_load_failed
import orbit.shared.generated.resources.space_items_loading
import orbit.shared.generated.resources.space_items_open
import orbit.shared.generated.resources.space_items_todo
import orbit.shared.generated.resources.turn_action_done
import orbit.shared.generated.resources.turn_label
import orbit.shared.generated.resources.turn_label_due
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Tasks destination, one list: the filters in a row, what is the user's turn, then To do and, folded to its count
 * at the bottom, Done. Tasks and lists are the same row.
 */
@Composable
internal fun SpaceItemsTab(
    spaceId: String,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceItemsViewModel = koinViewModel(key = "items-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
            for (filter in ItemFilter.entries) {
                OrbitFilterChip(
                    label = stringResource(filter.label),
                    selected = filter == state.filter,
                    onClick = { viewModel.selectFilter(filter) },
                )
            }
        }
        when {
            state.loading -> OrbitLoadingState(stringResource(Res.string.space_items_loading), Modifier.fillMaxWidth())
            state.loadFailed -> OrbitErrorState(
                title = stringResource(Res.string.space_items_load_failed),
                body = stringResource(Res.string.load_failed_body),
                retryText = stringResource(Res.string.retry),
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxWidth(),
            )

            state.isEmpty -> OrbitEmptyState(
                title = stringResource(Res.string.space_items_empty_title),
                body = stringResource(Res.string.space_items_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
        state.turn?.let { turn -> TurnCard(turn, state, viewModel::finishTurn, onOpen = { onOpenItem(turn.id) }) }
        if (state.todo.isNotEmpty()) {
            OrbitSectionHeader(
                title = stringResource(Res.string.space_items_todo),
                modifier = Modifier.fillMaxWidth(),
                count = pluralStringResource(Res.plurals.space_items_open, state.todo.size, state.todo.size),
            )
            Rows(state.todo, state, viewModel, onOpenItem)
        }
        if (state.done.isNotEmpty()) {
            OrbitSectionHeader(
                title = stringResource(Res.string.space_items_done),
                modifier = Modifier.fillMaxWidth(),
                count = state.done.size.toString(),
                action = stringResource(if (state.showingDone) Res.string.hide else Res.string.show),
                onAction = viewModel::toggleDoneSection,
            )
            if (state.showingDone) Rows(state.done, state, viewModel, onOpenItem)
        }
    }
}

@Composable
private fun Rows(items: List<SpaceItem>, state: SpaceItemsUiState, viewModel: SpaceItemsViewModel, onOpen: (String) -> Unit) {
    OrbitGroup(items, key = { it.id }, modifier = Modifier.fillMaxWidth()) { item ->
        SpaceItemRow(
            item = item,
            today = state.today,
            onClick = { onOpen(item.id) },
            onDoneChange = { done -> viewModel.toggleDone(item.id, done) },
            submitting = state.submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TurnCard(turn: SpaceItem, state: SpaceItemsUiState, onAction: () -> Unit, onOpen: () -> Unit) {
    val due = turn.dueDate?.let { itemDueLabel(it, state.today) }
    OrbitTurnCard(
        label = due?.let { stringResource(Res.string.turn_label_due, it) } ?: stringResource(Res.string.turn_label),
        title = turn.title,
        actionText = stringResource(Res.string.turn_action_done),
        onAction = onAction,
        onOpen = onOpen,
        initials = state.userInitials.ifEmpty { null },
        photoUrl = state.userPhotoUrl,
        enabled = !state.submitting,
        modifier = Modifier.fillMaxWidth(),
    )
}
