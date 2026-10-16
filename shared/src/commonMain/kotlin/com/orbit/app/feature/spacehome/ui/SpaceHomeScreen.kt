package com.orbit.app.feature.spacehome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatarStack
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitExtendedFab
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.spacehome.SpaceHomeTab
import com.orbit.app.feature.spacehome.SpaceHomeUiState
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.feature.spacehome.items.SpaceItemsTab
import com.orbit.app.feature.spacehome.members.SpaceMembersTab
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_add
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.ic_swap_horiz
import orbit.shared.generated.resources.space_agenda_empty_body
import orbit.shared.generated.resources.space_agenda_empty_title
import orbit.shared.generated.resources.space_bills_due
import orbit.shared.generated.resources.space_events_today
import orbit.shared.generated.resources.space_new_item
import orbit.shared.generated.resources.space_notifications
import orbit.shared.generated.resources.space_open_tasks
import orbit.shared.generated.resources.space_switch
import orbit.shared.generated.resources.space_today
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Home of a space, laid out as in the Sprint 1 mockups: its members, a summary and the items, agenda
 * and members tabs. The items and members tabs load themselves. Placeholders until their
 * features land: notifications, New item, the bills and events counts, and the agenda.
 * [onSwitchSpace] goes back to the list of spaces.
 */
@Composable
fun SpaceHomeScreen(
    spaceId: String,
    onSwitchSpace: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: SpaceHomeViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = state.name,
                actions = {
                    OrbitIconButton(Res.drawable.ic_swap_horiz, stringResource(Res.string.space_switch), onSwitchSpace)
                    // Placeholder until notifications land.
                    OrbitIconButton(
                        icon = Res.drawable.ic_notifications,
                        contentDescription = stringResource(Res.string.space_notifications),
                        onClick = {},
                        enabled = false,
                    )
                },
            )
        },
        floatingActionButton = {
            // Placeholder until the create-item flow (#36).
            OrbitExtendedFab(
                text = stringResource(Res.string.space_new_item),
                icon = Res.drawable.ic_add,
                onClick = {},
            )
        },
        scrollable = true,
    ) {
        if (state.memberInitials.isNotEmpty()) {
            OrbitAvatarStack(state.memberInitials)
        }
        Summary(state)
        PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
            for (tab in SpaceHomeTab.entries) {
                Tab(
                    selected = tab == state.tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = { Text(stringResource(tab.label)) },
                )
            }
        }
        when (state.tab) {
            SpaceHomeTab.ITEMS -> SpaceItemsTab(spaceId, onOpenItem)
            SpaceHomeTab.AGENDA -> Agenda()
            SpaceHomeTab.MEMBERS -> SpaceMembersTab(spaceId)
        }
    }
}

@Composable
private fun Agenda() {
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Text(stringResource(Res.string.space_today), style = MaterialTheme.typography.titleMedium)
        OrbitEmptyState(
            title = stringResource(Res.string.space_agenda_empty_title),
            body = stringResource(Res.string.space_agenda_empty_body),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Summary(state: SpaceHomeUiState) {
    OrbitCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Stat(state.openTasks, Res.string.space_open_tasks, Modifier.weight(1f))
            VerticalDivider(color = MaterialTheme.colorScheme.outline)
            Stat(state.billsDue, Res.string.space_bills_due, Modifier.weight(1f))
            VerticalDivider(color = MaterialTheme.colorScheme.outline)
            Stat(state.eventsToday, Res.string.space_events_today, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(count: Int, label: StringResource, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
