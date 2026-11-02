package com.orbit.app.feature.spacehome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatarButton
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitExtendedFab
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitNavigationBar
import com.orbit.app.core.designsystem.component.OrbitNavigationDestination
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitSegmentedTabs
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.notification.permission.RequireNotificationPermission
import com.orbit.app.domain.model.Preset
import com.orbit.app.feature.item.label
import com.orbit.app.feature.spacehome.SpaceHomeTab
import com.orbit.app.feature.spacehome.SpaceTasksView
import com.orbit.app.feature.spacehome.SpaceHomeUiState
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.feature.spacehome.items.SpaceItemsTab
import com.orbit.app.feature.spacehome.members.SpaceMembersRow
import com.orbit.app.feature.spacehome.settings.SpaceSettings
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.ic_add
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.space_agenda_empty_body
import orbit.shared.generated.resources.space_agenda_empty_title
import orbit.shared.generated.resources.space_back
import orbit.shared.generated.resources.space_expenses_empty_body
import orbit.shared.generated.resources.space_expenses_empty_title
import orbit.shared.generated.resources.space_new_item
import orbit.shared.generated.resources.space_new_item_create
import orbit.shared.generated.resources.space_new_item_failed
import orbit.shared.generated.resources.space_new_item_name
import orbit.shared.generated.resources.space_today
import orbit.shared.generated.resources.spaces_members
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * A space (ADR 0005): its name and member count in the top bar, which opens the list of spaces through [onBack],
 * as does leaving the space, and its destinations in the bottom bar. Home has the members, Tasks the items and the
 * agenda behind segmented tabs, and Expenses waits for #54. The member row and the items load themselves; "New item"
 * creates one from a preset and opens it. The agenda is a placeholder until #38. The account avatar calls
 * [onOpenAccount].
 */
@Composable
fun SpaceHomeScreen(
    spaceId: String,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: SpaceHomeViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = state.name,
                subtitle = state.memberCount.takeIf { it > 0 }?.let {
                    pluralStringResource(Res.plurals.spaces_members, it, it)
                },
                onTitleClick = onBack,
                onTitleClickLabel = stringResource(Res.string.space_back),
                actions = {
                    SpaceSettings(spaceId, onGone = onBack)
                    OrbitAvatarButton(
                        initials = state.userInitials,
                        photoUrl = state.userPhotoUrl,
                        accent = MaterialTheme.colorScheme.primary,
                        contentDescription = stringResource(Res.string.account_title),
                        onClick = onOpenAccount,
                    )
                },
            )
        },
        bottomBar = {
            OrbitNavigationBar(
                destinations = SpaceHomeTab.entries.map { OrbitNavigationDestination(stringResource(it.label), it.icon) },
                selectedIndex = state.tab.ordinal,
                onSelect = { viewModel.selectTab(SpaceHomeTab.entries[it]) },
            )
        },
        floatingActionButton = if (state.canAddItem) {
            {
                OrbitExtendedFab(
                    text = stringResource(Res.string.space_new_item),
                    icon = Res.drawable.ic_add,
                    onClick = viewModel::openNewItem,
                )
            }
        } else {
            null
        },
        scrollable = true,
    ) {
        when (state.tab) {
            SpaceHomeTab.HOME -> SpaceMembersRow(spaceId, modifier = Modifier.fillMaxWidth())
            SpaceHomeTab.TASKS -> {
                OrbitSegmentedTabs(
                    options = SpaceTasksView.entries.map { stringResource(it.label) },
                    selectedIndex = state.tasksView.ordinal,
                    onSelect = { viewModel.selectTasksView(SpaceTasksView.entries[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
                when (state.tasksView) {
                    SpaceTasksView.ITEMS -> SpaceItemsTab(spaceId, onOpenItem)
                    SpaceTasksView.AGENDA -> Agenda()
                }
            }

            SpaceHomeTab.EXPENSES -> OrbitEmptyState(
                title = stringResource(Res.string.space_expenses_empty_title),
                body = stringResource(Res.string.space_expenses_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (state.newItemOpen) {
        NewItemDialog(state, viewModel)
    }
    if (state.askNotifications) {
        RequireNotificationPermission(onResult = { viewModel.onNotificationsAnswered() })
    }
    state.itemToOpen?.let { itemId ->
        LaunchedEffect(itemId) {
            onOpenItem(itemId)
            viewModel.onCreatedItemOpened()
        }
    }
}

/** Picks a preset and names the item, which opens once created. */
@Composable
private fun NewItemDialog(state: SpaceHomeUiState, viewModel: SpaceHomeViewModel) {
    OrbitDialog(
        title = stringResource(Res.string.space_new_item),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::dismissNewItem,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
            for (preset in Preset.entries) {
                OrbitFilterChip(
                    label = stringResource(preset.label),
                    selected = preset == state.newItemPreset,
                    onClick = { viewModel.selectNewItemPreset(preset) },
                )
            }
        }
        OrbitTextField(
            value = state.newItemTitle,
            onValueChange = viewModel::onNewItemTitleChange,
            placeholder = stringResource(Res.string.space_new_item_name),
            modifier = Modifier.fillMaxWidth(),
            isError = state.createFailed,
            supportingText = if (state.createFailed) stringResource(Res.string.space_new_item_failed) else null,
        )
        OrbitButton(
            text = stringResource(Res.string.space_new_item_create),
            onClick = viewModel::createItem,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canCreate,
        )
    }
}

@Composable
private fun Agenda() {
    Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        OrbitSectionHeader(
            title = stringResource(Res.string.space_today),
            icon = Res.drawable.ic_event,
            modifier = Modifier.fillMaxWidth(),
        )
        OrbitEmptyState(
            title = stringResource(Res.string.space_agenda_empty_title),
            body = stringResource(Res.string.space_agenda_empty_body),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
