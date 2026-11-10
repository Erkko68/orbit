package com.orbit.app.feature.spacehome.calendar

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
import com.orbit.app.core.designsystem.component.OrbitMonth
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.item.ui.itemDueLabel
import com.orbit.app.feature.spacehome.items.SpaceItem
import com.orbit.app.feature.spacehome.items.SpaceItemRow
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.calendar_day_empty_body
import orbit.shared.generated.resources.calendar_day_empty_title
import orbit.shared.generated.resources.calendar_marked
import orbit.shared.generated.resources.calendar_next_month
import orbit.shared.generated.resources.calendar_overdue
import orbit.shared.generated.resources.calendar_previous_month
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_agenda_empty_body
import orbit.shared.generated.resources.space_agenda_empty_title
import orbit.shared.generated.resources.space_items_load_failed
import orbit.shared.generated.resources.space_items_loading
import orbit.shared.generated.resources.space_items_open
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The Calendar destination under the headline: the month, with the items of the day picked under it, or the list by
 * day to the end of the week, overdue first. The chips switch between them. A row opens its item through [onOpenItem].
 */
@Composable
internal fun SpaceCalendarTab(
    spaceId: String,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceCalendarViewModel = koinViewModel(key = "calendar-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
            for (view in CalendarView.entries) {
                OrbitFilterChip(
                    label = stringResource(view.label),
                    selected = view == state.view,
                    onClick = { viewModel.selectView(view) },
                    icon = view.icon,
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

            state.view == CalendarView.MONTH -> Month(state, viewModel, onOpenItem)
            else -> Agenda(state, viewModel, onOpenItem)
        }
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
    }
}

@Composable
private fun Month(state: SpaceCalendarUiState, viewModel: SpaceCalendarViewModel, onOpen: (String) -> Unit) {
    val month = state.month ?: return
    val selected = state.selected ?: return
    OrbitMonth(
        month = month,
        today = state.today,
        selected = selected,
        marked = state.marked,
        markedLabel = stringResource(Res.string.calendar_marked),
        onSelect = viewModel::selectDay,
        previousLabel = stringResource(Res.string.calendar_previous_month),
        onPrevious = viewModel::previousMonth,
        nextLabel = stringResource(Res.string.calendar_next_month),
        onNext = viewModel::nextMonth,
        modifier = Modifier.fillMaxWidth(),
    )
    Section(itemDueLabel(selected, state.today), state.selectedItems, state, viewModel, onOpen)
    if (state.selectedItems.isEmpty()) {
        OrbitEmptyState(
            title = stringResource(Res.string.calendar_day_empty_title),
            body = stringResource(Res.string.calendar_day_empty_body),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Agenda(state: SpaceCalendarUiState, viewModel: SpaceCalendarViewModel, onOpen: (String) -> Unit) {
    if (state.agendaEmpty) {
        OrbitEmptyState(
            title = stringResource(Res.string.space_agenda_empty_title),
            body = stringResource(Res.string.space_agenda_empty_body),
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    if (state.overdue.isNotEmpty()) Section(stringResource(Res.string.calendar_overdue), state.overdue, state, viewModel, onOpen)
    for (day in state.days) {
        Section(itemDueLabel(day.date, state.today), day.items, state, viewModel, onOpen)
    }
}

@Composable
private fun Section(
    title: String,
    items: List<SpaceItem>,
    state: SpaceCalendarUiState,
    viewModel: SpaceCalendarViewModel,
    onOpen: (String) -> Unit,
) {
    OrbitSectionHeader(
        title = title,
        modifier = Modifier.fillMaxWidth(),
        count = pluralStringResource(Res.plurals.space_items_open, items.size, items.size),
    )
    OrbitGroup(items, key = { it.id }, modifier = Modifier.fillMaxWidth()) { item ->
        SpaceItemRow(
            item = item,
            today = state.today,
            onClick = { onOpen(item.id) },
            onDoneChange = { viewModel.complete(item.id) },
            submitting = state.submitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
