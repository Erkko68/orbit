package com.orbit.app.feature.spacehome.items

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitListCard
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.space_items_empty_body
import orbit.shared.generated.resources.space_items_empty_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The items of a space under their filter chips, loaded by the tab's own view model. */
@Composable
internal fun SpaceItemsTab(
    spaceId: String,
    modifier: Modifier = Modifier,
    viewModel: SpaceItemsViewModel = koinViewModel(key = spaceId) { parametersOf(spaceId) },
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
        if (state.items.isEmpty()) {
            OrbitEmptyState(
                title = stringResource(Res.string.space_items_empty_title),
                body = stringResource(Res.string.space_items_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        for (item in state.items) {
            OrbitListCard(item.title, stringResource(item.type.label), item.type.icon, Modifier.fillMaxWidth())
        }
    }
}
