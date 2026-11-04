package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitAddRow
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitColorHeader
import com.orbit.app.core.designsystem.component.OrbitDetailsField
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitEntryRow
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFaceStack
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.component.orbitGroup
import com.orbit.app.core.designsystem.component.orbitItem
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.notification.permission.RequireNotificationPermission
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.feature.item.history.ItemHistory
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.checklist_check_entry
import orbit.shared.generated.resources.checklist_uncheck_entry
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_delete
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_group
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_add_date
import orbit.shared.generated.resources.item_add_reminder
import orbit.shared.generated.resources.item_back
import orbit.shared.generated.resources.item_checklist
import orbit.shared.generated.resources.item_complete
import orbit.shared.generated.resources.item_create
import orbit.shared.generated.resources.item_delete
import orbit.shared.generated.resources.item_description
import orbit.shared.generated.resources.item_load_failed
import orbit.shared.generated.resources.item_load_failed_body
import orbit.shared.generated.resources.item_name
import orbit.shared.generated.resources.item_loading
import orbit.shared.generated.resources.item_name_placeholder
import orbit.shared.generated.resources.item_not_found
import orbit.shared.generated.resources.item_not_found_body
import orbit.shared.generated.resources.item_reminder
import orbit.shared.generated.resources.item_reopen
import orbit.shared.generated.resources.item_when
import orbit.shared.generated.resources.item_who
import orbit.shared.generated.resources.item_who_empty
import orbit.shared.generated.resources.list_add_entry
import orbit.shared.generated.resources.module_assignment_anyone
import orbit.shared.generated.resources.module_checklist_quantity
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_item_entry_count
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val ADD_ENTRY = "add-entry"

/** The rows that say who has the item, when it is due and when to remind. */
private enum class ItemRow { WHO, WHEN, REMINDER }

/**
 * One item, the same form for all of them: a colour block with its name typed in place, who has it, when and the
 * reminder, the description, the checklist with its add row last, and the history. One button stays at the bottom: Create on a new
 * item, which writes nothing until then, and Mark done or Reopen on a written one without entries, since a checklist
 * finishes by itself (ADR 0008). A written item saves each change as it is made. An empty [itemId] is a new item.
 */
@Composable
fun ItemScreen(
    spaceId: String,
    itemId: String,
    onBack: () -> Unit,
    viewModel: ItemViewModel = koinViewModel { parametersOf(spaceId, itemId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted, state.created) {
        if (state.deleted || state.created) onBack()
    }
    // Every way out keeps what was typed on a written item, the swipe back included.
    DisposableEffect(viewModel) {
        onDispose { viewModel.saveDrafts() }
    }
    OrbitSpaceColor(state.accent) {
        OrbitScreen(
            topBar = {
                if (state.loaded) {
                    ItemHeader(state, viewModel, onBack)
                } else {
                    OrbitTopBar(
                        title = state.spaceName.orEmpty(),
                        navigationIcon = { OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.item_back), onBack) },
                    )
                }
            },
            primaryAction = primaryAction(state, viewModel),
            collapsingTopBar = true,
            list = { itemBody(state, viewModel, spaceId, itemId) },
        )
        state.scheduleEditor?.let { editor -> ScheduleEditorSheet(editor, state, viewModel) }
        state.checklistEditor?.let { editor -> ChecklistEditorSheet(editor, state, viewModel) }
        if (state.pickingMember) AssigneeSheet(state, viewModel)
        if (state.editingReminder) ReminderSheet(state, viewModel)
        if (state.confirmingDelete) DeleteItemSheet(state, viewModel)
    }
    if (state.askNotifications) {
        RequireNotificationPermission(onResult = { viewModel.onNotificationsAnswered() })
    }
}

/**
 * The item's colour block: the space it is in, the way back, its name typed in place with the pencil that says so, and
 * the delete button for who can delete. The rules refuse anyone else, so the button has nothing to say to them.
 */
@Composable
private fun ItemHeader(state: ItemUiState, viewModel: ItemViewModel, onBack: () -> Unit) {
    OrbitColorHeader(
        title = state.shownTitle,
        label = state.spaceName.orEmpty(),
        backDescription = stringResource(Res.string.item_back),
        onBack = onBack,
        trailing = if (state.canDelete) {
            { OrbitIconButton(Res.drawable.ic_delete, stringResource(Res.string.item_delete), viewModel::openDelete) }
        } else {
            null
        },
        onTitleChange = viewModel::onTitleChange,
        onTitleDone = viewModel::saveDrafts,
        titleDescription = stringResource(Res.string.item_name),
        titlePlaceholder = stringResource(Res.string.item_name_placeholder),
        focusTitle = state.isNew,
    )
}

/** Create on a new item; Mark done, or quietly Reopen, on a written one without entries. */
private fun primaryAction(state: ItemUiState, viewModel: ItemViewModel): (@Composable () -> Unit)? = when {
    state.isNew -> {
        { OrbitButton(stringResource(Res.string.item_create), viewModel::create, Modifier.fillMaxWidth(), state.canCreate) }
    }

    state.showsDone && state.isDone -> {
        { OrbitTonalButton(stringResource(Res.string.item_reopen), viewModel::reopen, Modifier.fillMaxWidth(), state.canSubmit) }
    }

    state.showsDone -> {
        { OrbitButton(stringResource(Res.string.item_complete), viewModel::complete, Modifier.fillMaxWidth(), state.canSubmit) }
    }

    else -> null
}

private fun LazyListScope.itemBody(state: ItemUiState, viewModel: ItemViewModel, spaceId: String, itemId: String) {
    when {
        state.loading -> item {
            OrbitLoadingState(stringResource(Res.string.item_loading), Modifier.fillMaxWidth())
        }

        state.loadFailed -> item {
            OrbitErrorState(
                title = stringResource(Res.string.item_load_failed),
                body = stringResource(Res.string.item_load_failed_body),
                retryText = stringResource(Res.string.retry),
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.notFound -> item {
            OrbitEmptyState(
                title = stringResource(Res.string.item_not_found),
                body = stringResource(Res.string.item_not_found_body),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        else -> {
            if (state.failed) {
                item(key = "failed") { OrbitErrorText(stringResource(Res.string.item_action_failed)) }
            }
            orbitGroup(ItemRow.entries, key = { it.name }) { row -> ItemActionRow(row, state, viewModel) }
            gap("description")
            item(key = "description") {
                OrbitDetailsField(
                    value = state.shownDescription,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = stringResource(Res.string.item_description),
                    onDone = viewModel::saveDrafts,
                    modifier = orbitItem(Modifier.fillMaxWidth()),
                )
            }
            checklist(state, viewModel)
            if (itemId.isNotEmpty()) {
                item(key = "history") { ItemHistory(spaceId, itemId, orbitItem(Modifier.fillMaxWidth())) }
            }
        }
    }
}

/** The gap between the sections of the form, which a lazy list does not space by itself. */
private fun LazyListScope.gap(before: String) = item(key = "gap-$before") { Spacer(Modifier.height(OrbitSpacing.md)) }

/** A row names what it holds and shows its value, or what tapping it adds while it has none. */
@Composable
private fun ItemActionRow(row: ItemRow, state: ItemUiState, viewModel: ItemViewModel) {
    val modifier = Modifier.fillMaxWidth()
    when (row) {
        ItemRow.WHO -> OrbitActionRow(
            title = stringResource(Res.string.item_who),
            supporting = when {
                state.isAnyones -> stringResource(Res.string.module_assignment_anyone)
                state.assigneeFaces.isEmpty() -> stringResource(Res.string.item_who_empty)
                else -> state.assigneeFaces.joinToString { it.name }
            },
            icon = Res.drawable.ic_group,
            onClick = viewModel::openMemberPicker,
            modifier = modifier,
            trailing = if (state.assigneeFaces.isEmpty()) null else ({ OrbitFaceStack(state.assigneeFaces.map { it.face() }) }),
        )

        ItemRow.WHEN -> OrbitActionRow(
            title = stringResource(Res.string.item_when),
            supporting = state.schedule?.let { itemScheduleLabel(it) } ?: stringResource(Res.string.item_add_date),
            icon = Res.drawable.ic_event,
            onClick = viewModel::editSchedule,
            modifier = modifier,
            shape = OrbitShape.Sunny,
        )

        ItemRow.REMINDER -> OrbitActionRow(
            title = stringResource(Res.string.item_reminder),
            supporting = itemReminderSummary(state.reminder) ?: stringResource(Res.string.item_add_reminder),
            icon = Res.drawable.ic_notifications,
            onClick = viewModel::openReminder,
            modifier = modifier,
            shape = OrbitShape.Clover,
        )
    }
}

/** Always there, under its header: the entries, what is left first, then the row that adds, as the list apps do. */
private fun LazyListScope.checklist(state: ItemUiState, viewModel: ItemViewModel) {
    item(key = "checklist") {
        OrbitSectionHeader(
            title = stringResource(Res.string.item_checklist),
            modifier = orbitItem(Modifier.fillMaxWidth()),
            count = if (state.hasEntries) {
                stringResource(Res.string.space_item_entry_count, state.checkedCount, state.entryCount)
            } else {
                null
            },
        )
    }
    orbitGroup<Any>(state.shownEntries + ADD_ENTRY, key = { (it as? ChecklistEntry)?.id ?: ADD_ENTRY }) { row ->
        if (row is ChecklistEntry) {
            EntryRow(row, viewModel)
        } else {
            OrbitAddRow(
                value = state.newEntry,
                onValueChange = viewModel::onNewEntryChange,
                placeholder = stringResource(Res.string.list_add_entry),
                onAdd = viewModel::addNewEntry,
                modifier = Modifier.fillMaxWidth(),
                canAdd = state.canAddEntry,
            )
        }
    }
}

@Composable
private fun EntryRow(entry: ChecklistEntry, viewModel: ItemViewModel) {
    OrbitEntryRow(
        text = entry.text,
        quantity = entry.quantity?.let { stringResource(Res.string.module_checklist_quantity, it) },
        checked = entry.checked,
        onCheckedChange = { viewModel.toggleChecklistEntry(entry.id) },
        checkDescription = stringResource(
            if (entry.checked) Res.string.checklist_uncheck_entry else Res.string.checklist_check_entry,
            entry.text,
        ),
        onClick = { viewModel.editChecklistEntry(entry.id) },
        modifier = Modifier.fillMaxWidth(),
    )
}
