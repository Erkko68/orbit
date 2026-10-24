package com.orbit.app.feature.spacehome.items

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_task
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_items_empty_body
import orbit.shared.generated.resources.space_items_empty_title
import orbit.shared.generated.resources.space_items_load_failed
import orbit.shared.generated.resources.space_items_loading
import orbit.shared.generated.resources.space_items_open
import orbit.shared.generated.resources.space_tab_items
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The items of a space under their section header and filter chips, loaded by the tab's own view model. */
@Composable
internal fun SpaceItemsTab(
    spaceId: String,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceItemsViewModel = koinViewModel(key = "items-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        OrbitSectionHeader(
            title = stringResource(Res.string.space_tab_items),
            icon = Res.drawable.ic_task,
            modifier = Modifier.fillMaxWidth(),
            count = state.openCount.takeIf { it > 0 }?.let {
                pluralStringResource(Res.plurals.space_items_open, it, it)
            },
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
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

            state.items.isEmpty() -> OrbitEmptyState(
                title = stringResource(Res.string.space_items_empty_title),
                body = stringResource(Res.string.space_items_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            for (item in state.items) {
                key(item.id) {
                    SpaceItemCard(
                        item = item,
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
