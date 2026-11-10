package com.orbit.app.feature.spacehome.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAddButton
import com.orbit.app.core.designsystem.component.OrbitAddMenu
import com.orbit.app.core.designsystem.component.OrbitAddMenuAction
import com.orbit.app.core.designsystem.component.OrbitAvatarButton
import com.orbit.app.core.designsystem.component.OrbitFaceStack
import com.orbit.app.core.designsystem.component.OrbitHero
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitNavigationBar
import com.orbit.app.core.designsystem.component.OrbitNavigationDestination
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.item.ui.face
import com.orbit.app.feature.item.ui.itemDateLabel
import com.orbit.app.feature.spacehome.SpaceHeadline
import com.orbit.app.feature.spacehome.SpaceHomeTab
import com.orbit.app.feature.spacehome.SpaceHomeUiState
import com.orbit.app.feature.spacehome.SpaceHomeViewModel
import com.orbit.app.feature.spacehome.calendar.SpaceCalendarTab
import com.orbit.app.feature.spacehome.expenses.SpaceExpensesTab
import com.orbit.app.feature.spacehome.items.SpaceItemsTab
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.expense_add
import orbit.shared.generated.resources.ic_sparkle
import orbit.shared.generated.resources.ic_task
import orbit.shared.generated.resources.ic_tune
import orbit.shared.generated.resources.space_assistant
import orbit.shared.generated.resources.space_back
import orbit.shared.generated.resources.space_headline_clear
import orbit.shared.generated.resources.space_headline_due
import orbit.shared.generated.resources.space_headline_empty
import orbit.shared.generated.resources.space_new_item
import orbit.shared.generated.resources.space_new_task
import orbit.shared.generated.resources.space_settings
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * A space (ADR 0005), painted in its colour: its name in the top bar, which opens the list of spaces through [onBack],
 * and its destinations in the bottom bar. Beside the name the members' faces and a settings button open the space's
 * settings through [onOpenSettings]. Calendar says where the user stands, with the month or the agenda under it. Tasks has the items, and
 * Expenses has the ledger. The components load themselves. The add button opens an expense on Expenses; elsewhere it opens a menu: a new item
 * by hand, or the assistant through [onOpenAssistant]. The account avatar calls [onOpenAccount].
 */
@Composable
fun SpaceHomeScreen(
    spaceId: String,
    onBack: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenItem: (String) -> Unit,
    onNewItem: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExpense: (String) -> Unit = {},
    onNewExpense: () -> Unit = {},
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
                        val settings = stringResource(Res.string.space_settings)
                        OrbitFaceStack(state.faces.map { it.face() }, onClick = onOpenSettings, clickLabel = settings)
                        OrbitIconButton(Res.drawable.ic_tune, settings, onOpenSettings)
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
            floatingActionButton = {
                if (state.canAddItem) {
                    OrbitAddMenu(
                        expanded = state.addMenuOpen,
                        onExpandedChange = { open -> if (open) viewModel.openAddMenu() else viewModel.closeAddMenu() },
                        contentDescription = stringResource(Res.string.space_new_item),
                        closeDescription = stringResource(Res.string.close),
                        actions = listOf(
                            OrbitAddMenuAction(stringResource(Res.string.space_new_task), Res.drawable.ic_task, onNewItem),
                            OrbitAddMenuAction(stringResource(Res.string.space_assistant), Res.drawable.ic_sparkle, onOpenAssistant),
                        ),
                    )
                } else {
                    OrbitAddButton(stringResource(Res.string.expense_add), onNewExpense)
                }
            },
            scrollable = true,
        ) {
            when (state.tab) {
                SpaceHomeTab.CALENDAR -> {
                    OrbitHero(
                        title = headlineText(state),
                        datum = state.today?.let { itemDateLabel(it) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SpaceCalendarTab(spaceId, onOpenItem, Modifier.fillMaxWidth())
                }

                SpaceHomeTab.TASKS -> SpaceItemsTab(spaceId, onOpenItem, Modifier.fillMaxWidth())

                SpaceHomeTab.EXPENSES -> SpaceExpensesTab(spaceId, onOpenExpense, Modifier.fillMaxWidth())
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
