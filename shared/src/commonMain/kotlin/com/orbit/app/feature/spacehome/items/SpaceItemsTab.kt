package com.orbit.app.feature.spacehome.items

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitListTile
import com.orbit.app.core.designsystem.component.OrbitListTileSlot
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitTurnCard
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.item.ui.floating
import com.orbit.app.feature.item.ui.itemDueLabel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.filter_lists
import orbit.shared.generated.resources.filter_tasks
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_item_entry_count
import orbit.shared.generated.resources.space_items_empty_body
import orbit.shared.generated.resources.space_items_empty_title
import orbit.shared.generated.resources.space_items_load_failed
import orbit.shared.generated.resources.space_items_loading
import orbit.shared.generated.resources.space_items_open
import orbit.shared.generated.resources.space_list_nothing_left
import orbit.shared.generated.resources.space_list_peek_more
import orbit.shared.generated.resources.turn_action_done
import orbit.shared.generated.resources.turn_action_undo
import orbit.shared.generated.resources.turn_finished
import orbit.shared.generated.resources.turn_label
import orbit.shared.generated.resources.turn_label_due
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val STAGGER = 2
private const val WIDE_TILE = 3f
private const val NARROW_TILE = 2f

/** The Tasks destination: the filters on the page, what is the user's turn, the lists as tiles, and the rest as cards. */
@Composable
internal fun SpaceItemsTab(
    spaceId: String,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceItemsViewModel = koinViewModel(key = "items-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
        ) {
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
        state.turn?.let { turn -> TurnCard(turn, state, viewModel::toggleTurn, onOpen = { onOpenItem(turn.id) }) }
        if (state.lists.isNotEmpty()) {
            OrbitSectionHeader(stringResource(Res.string.filter_lists), Modifier.fillMaxWidth())
            ListTiles(state.lists, onOpenItem)
        }
        if (state.rows.isNotEmpty()) {
            OrbitSectionHeader(
                title = stringResource(Res.string.filter_tasks),
                modifier = Modifier.fillMaxWidth(),
                count = pluralStringResource(Res.plurals.space_items_open, state.openRows, state.openRows),
            )
            for (item in state.rows) {
                key(item.id) {
                    SpaceItemRow(
                        item = item,
                        today = state.today,
                        onClick = { onOpenItem(item.id) },
                        onDoneChange = { done -> viewModel.toggleDone(item.id, done) },
                        submitting = state.submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TurnCard(turn: SpaceItem, state: SpaceItemsUiState, onAction: () -> Unit, onOpen: () -> Unit) {
    val due = turn.dueDate?.let { itemDueLabel(it, state.today) }
    OrbitTurnCard(
        label = when {
            turn.done -> stringResource(Res.string.turn_finished)
            due != null -> stringResource(Res.string.turn_label_due, due)
            else -> stringResource(Res.string.turn_label)
        },
        title = turn.title,
        actionText = stringResource(if (turn.done) Res.string.turn_action_undo else Res.string.turn_action_done),
        finished = turn.done,
        onAction = onAction,
        onOpen = onOpen,
        initials = state.userInitials.ifEmpty { null },
        photoUrl = state.userPhotoUrl,
        enabled = !state.submitting,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The lists in pairs that touch, a wide one then a narrow one. One left over takes the whole row. */
@Composable
private fun ListTiles(lists: List<SpaceItem>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
        for (row in lists.chunked(STAGGER)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            ) {
                val first = row.first()
                val second = row.getOrNull(1)
                ListTile(
                    list = first,
                    slot = if (second == null) OrbitListTileSlot.Alone else OrbitListTileSlot.Start,
                    onOpen = onOpen,
                    modifier = Modifier.weight(WIDE_TILE).fillMaxHeight(),
                )
                if (second != null) {
                    ListTile(second, OrbitListTileSlot.End, onOpen, Modifier.weight(NARROW_TILE).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun ListTile(list: SpaceItem, slot: OrbitListTileSlot, onOpen: (String) -> Unit, modifier: Modifier) {
    val checklist = list.checklist
    OrbitListTile(
        title = list.title,
        peek = checklist?.peek?.entries.orEmpty(),
        peekNote = checklist?.peek?.let { peekNote(it) },
        faces = list.faces.map { it.floating() },
        count = stringResource(Res.string.space_item_entry_count, checklist?.done ?: 0, checklist?.total ?: 0),
        progress = checklist?.progress ?: 0f,
        onClick = { onOpen(list.id) },
        slot = slot,
        modifier = modifier,
    )
}

@Composable
private fun peekNote(peek: SpaceItemPeek): String? = when {
    peek.entries.isEmpty() -> stringResource(Res.string.space_list_nothing_left)
    peek.more > 0 -> stringResource(Res.string.space_list_peek_more, peek.more)
    else -> null
}
