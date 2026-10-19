package com.orbit.app.feature.spacehome.items

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitListCard
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_complete
import orbit.shared.generated.resources.item_reopen
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_items_empty_body
import orbit.shared.generated.resources.space_items_empty_title
import orbit.shared.generated.resources.space_items_load_failed
import orbit.shared.generated.resources.space_items_loading
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The items of a space under their filter chips, loaded by the tab's own view model. */
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
        for (item in state.items) {
            OrbitListCard(
                title = item.title,
                supporting = stringResource(item.label),
                icon = item.icon,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenItem(item.id) },
                titleDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                trailing = {
                    val completeDescription = stringResource(if (item.done) Res.string.item_reopen else Res.string.item_complete)
                    Checkbox(
                        checked = item.done,
                        onCheckedChange = { checked -> viewModel.toggleDone(item.id, checked) },
                        enabled = !state.submitting,
                        modifier = Modifier.semantics { contentDescription = completeDescription },
                    )
                },
            )
        }
    }
}
