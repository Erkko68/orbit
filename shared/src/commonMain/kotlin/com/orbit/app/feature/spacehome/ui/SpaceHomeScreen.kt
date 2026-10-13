package com.orbit.app.feature.spacehome.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitExtendedFab
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.spacehome.ItemFilter
import com.orbit.app.feature.spacehome.SpaceHomeUiState
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.feature.spacehome.SpaceItem
import com.orbit.app.feature.spacehome.SpaceMember
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_add
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.ic_swap_horiz
import orbit.shared.generated.resources.space_agenda_empty_body
import orbit.shared.generated.resources.space_agenda_empty_title
import orbit.shared.generated.resources.space_bills_due
import orbit.shared.generated.resources.space_events_today
import orbit.shared.generated.resources.space_items_empty_body
import orbit.shared.generated.resources.space_items_empty_title
import orbit.shared.generated.resources.space_more_members
import orbit.shared.generated.resources.space_new_item
import orbit.shared.generated.resources.space_notifications
import orbit.shared.generated.resources.space_open_tasks
import orbit.shared.generated.resources.space_switch
import orbit.shared.generated.resources.space_tab_agenda
import orbit.shared.generated.resources.space_tab_items
import orbit.shared.generated.resources.space_tab_members
import orbit.shared.generated.resources.space_today
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val VISIBLE_AVATARS = 4

private enum class SpaceHomeTab(val label: StringResource) {
    ITEMS(Res.string.space_tab_items),
    AGENDA(Res.string.space_tab_agenda),
    MEMBERS(Res.string.space_tab_members),
}

/**
 * Home of a space, laid out as in the Sprint 1 mockups: its members, a summary and the items, agenda
 * and members tabs, with the item list's filter chips on the items tab. Placeholders until their
 * features land: notifications, New item, the bills and events counts, and the agenda.
 * [onSwitchSpace] goes back to the list of spaces.
 */
@Composable
fun SpaceHomeScreen(
    spaceId: String,
    onSwitchSpace: () -> Unit,
    viewModel: SpaceHomeViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(SpaceHomeTab.ITEMS) }
    val accents = OrbitTheme.colors.spaceAccents
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = state.name,
                actions = {
                    IconButton(onClick = onSwitchSpace) {
                        Icon(painterResource(Res.drawable.ic_swap_horiz), stringResource(Res.string.space_switch))
                    }
                    // Placeholder until notifications land.
                    IconButton(onClick = {}, enabled = false) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_notifications),
                            contentDescription = stringResource(Res.string.space_notifications),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            // Placeholder until the create-item flow (#36).
            OrbitExtendedFab(
                text = stringResource(Res.string.space_new_item),
                icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                onClick = {},
            )
        },
    ) {
        Column(
            // Room to scroll the last card above the New item button.
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = OrbitSpacing.xl * 3),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
        ) {
            if (state.members.isNotEmpty()) {
                MemberAvatars(state.members, accents)
            }
            Summary(state)
            PrimaryTabRow(selectedTabIndex = selected.ordinal) {
                for (tab in SpaceHomeTab.entries) {
                    Tab(
                        selected = tab == selected,
                        onClick = { selected = tab },
                        text = { Text(stringResource(tab.label)) },
                    )
                }
            }
            when (selected) {
                SpaceHomeTab.ITEMS -> Items(state, onSelect = viewModel::select)
                SpaceHomeTab.AGENDA -> Agenda()
                SpaceHomeTab.MEMBERS -> MemberList(state.members, accents)
            }
        }
    }
}

@Composable
private fun Items(state: SpaceHomeUiState, onSelect: (ItemFilter) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        ) {
            for (filter in ItemFilter.entries) {
                OrbitFilterChip(
                    label = stringResource(filter.label),
                    selected = filter == state.filter,
                    onClick = { onSelect(filter) },
                )
            }
        }
        if (state.items.isEmpty()) {
            EmptyState(Res.string.space_items_empty_title, Res.string.space_items_empty_body)
        }
        for (item in state.items) {
            ItemRow(item)
        }
    }
}

@Composable
private fun ItemRow(item: SpaceItem) {
    OrbitCard(Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(item.type.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(item.type.label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Agenda() {
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        Text(stringResource(Res.string.space_today), style = MaterialTheme.typography.titleMedium)
        EmptyState(Res.string.space_agenda_empty_title, Res.string.space_agenda_empty_body)
    }
}

@Composable
private fun MemberAvatars(members: List<SpaceMember>, accents: List<Color>) {
    // Overlapping, as in the mockup.
    Row(horizontalArrangement = Arrangement.spacedBy(-OrbitSpacing.sm)) {
        members.take(VISIBLE_AVATARS).forEachIndexed { index, member ->
            OrbitAvatar(member.initials, accents[index % accents.size])
        }
        val hidden = members.size - VISIBLE_AVATARS
        if (hidden > 0) {
            OrbitAvatar(stringResource(Res.string.space_more_members, hidden), MaterialTheme.colorScheme.outline)
        }
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

@Composable
private fun EmptyState(title: StringResource, body: StringResource) {
    OrbitCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MemberList(members: List<SpaceMember>, accents: List<Color>) {
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
        members.forEachIndexed { index, member ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrbitAvatar(member.initials, accents[index % accents.size])
                Column {
                    Text(member.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(member.role),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
