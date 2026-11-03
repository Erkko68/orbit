package com.orbit.app.feature.spacehome.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAddButton
import com.orbit.app.core.designsystem.component.OrbitAvatarButton
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitHero
import com.orbit.app.core.designsystem.component.OrbitNavigationBar
import com.orbit.app.core.designsystem.component.OrbitNavigationDestination
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.item.ui.itemDateLabel
import com.orbit.app.feature.spacehome.SpaceHeadline
import com.orbit.app.feature.spacehome.SpaceHomeTab
import com.orbit.app.feature.spacehome.SpaceHomeUiState
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.feature.spacehome.items.SpaceItemsTab
import com.orbit.app.feature.spacehome.members.SpaceMembersRow
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.space_agenda_empty_body
import orbit.shared.generated.resources.space_agenda_empty_title
import orbit.shared.generated.resources.space_back
import orbit.shared.generated.resources.space_expenses_empty_body
import orbit.shared.generated.resources.space_expenses_empty_title
import orbit.shared.generated.resources.space_headline_clear
import orbit.shared.generated.resources.space_headline_due
import orbit.shared.generated.resources.space_headline_empty
import orbit.shared.generated.resources.space_new_item
import orbit.shared.generated.resources.space_tab_expenses
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * A space (ADR 0005), painted in its colour: its name in the top bar, which opens the list of spaces through [onBack],
 * as does leaving the space, and its destinations in the bottom bar. Home says where the user stands, with the members
 * and the settings under it and the agenda (#38) after. Tasks has the items, and Expenses waits for #54. The member
 * row and the items load themselves. The add button opens a new item through [onNewItem]. The account avatar calls
 * [onOpenAccount].
 */
@Composable
fun SpaceHomeScreen(
    spaceId: String,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenItem: (String) -> Unit,
    onNewItem: () -> Unit,
    viewModel: SpaceHomeViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitSpaceColor(state.accent) {
        OrbitScreen(
            topBar = {
                OrbitTopBar(
                    title = state.name,
                    onTitleClick = onBack,
                    onTitleClickLabel = stringResource(Res.string.space_back),
                    actions = {
                        OrbitAvatarButton(
                            initials = state.userInitials,
                            photoUrl = state.userPhotoUrl,
                            accent = OrbitTheme.accent.fill,
                            contentDescription = stringResource(Res.string.account_title),
                            onClick = onOpenAccount,
                        )
                    },
                )
            },
            bottomBar = {
                OrbitNavigationBar(
                    destinations = SpaceHomeTab.entries.map {
                        OrbitNavigationDestination(stringResource(it.label), it.icon, it.selectedIcon)
                    },
                    selectedIndex = state.tab.ordinal,
                    onSelect = { viewModel.selectTab(SpaceHomeTab.entries[it]) },
                )
            },
            floatingActionButton = if (state.canAddItem) {
                { OrbitAddButton(stringResource(Res.string.space_new_item), onNewItem) }
            } else {
                null
            },
            scrollable = true,
        ) {
            when (state.tab) {
                SpaceHomeTab.HOME -> {
                    OrbitHero(
                        title = headlineText(state),
                        datum = state.today?.let { itemDateLabel(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SpaceMembersRow(spaceId, onGone = onBack, modifier = Modifier.fillMaxWidth())
                    OrbitEmptyState(
                        title = stringResource(Res.string.space_agenda_empty_title),
                        body = stringResource(Res.string.space_agenda_empty_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SpaceHomeTab.TASKS -> SpaceItemsTab(spaceId, onOpenItem, Modifier.fillMaxWidth())

                SpaceHomeTab.EXPENSES -> {
                    OrbitHero(stringResource(Res.string.space_tab_expenses), Modifier.fillMaxWidth())
                    OrbitEmptyState(
                        title = stringResource(Res.string.space_expenses_empty_title),
                        body = stringResource(Res.string.space_expenses_empty_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun headlineText(state: SpaceHomeUiState): String = when (val headline = state.headline) {
    SpaceHeadline.Empty -> stringResource(Res.string.space_headline_empty)
    SpaceHeadline.AllClear -> stringResource(Res.string.space_headline_clear)
    is SpaceHeadline.Due -> pluralStringResource(Res.plurals.space_headline_due, headline.count, headline.count)
    null -> state.name
}
