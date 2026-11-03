package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitAddRow
import com.orbit.app.core.designsystem.component.OrbitColorHeader
import com.orbit.app.core.designsystem.component.OrbitDetailsField
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitEntryRow
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFloatingFace
import com.orbit.app.core.designsystem.component.OrbitFloatingFaces
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitHeaderAction
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitNoteField
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.component.orbitGroup
import com.orbit.app.core.designsystem.component.orbitItem
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.InkSoft
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.notification.permission.RequireNotificationPermission
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.feature.item.history.ItemHistory
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.assignment_pick_member
import orbit.shared.generated.resources.checklist_check_entry
import orbit.shared.generated.resources.checklist_uncheck_entry
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_check
import orbit.shared.generated.resources.ic_delete
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_list
import orbit.shared.generated.resources.ic_notifications
import orbit.shared.generated.resources.ic_person_add
import orbit.shared.generated.resources.ic_undo
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_add_checklist
import orbit.shared.generated.resources.item_add_date
import orbit.shared.generated.resources.item_add_reminder
import orbit.shared.generated.resources.item_back
import orbit.shared.generated.resources.item_complete
import orbit.shared.generated.resources.item_delete
import orbit.shared.generated.resources.item_description
import orbit.shared.generated.resources.item_details
import orbit.shared.generated.resources.item_load_failed
import orbit.shared.generated.resources.item_load_failed_body
import orbit.shared.generated.resources.item_loading
import orbit.shared.generated.resources.item_name
import orbit.shared.generated.resources.item_name_placeholder
import orbit.shared.generated.resources.item_not_found
import orbit.shared.generated.resources.item_not_found_body
import orbit.shared.generated.resources.item_reopen
import orbit.shared.generated.resources.list_add_entry
import orbit.shared.generated.resources.module_checklist_quantity
import orbit.shared.generated.resources.module_checklist_remove
import orbit.shared.generated.resources.retry
import orbit.shared.generated.resources.space_item_entry_count
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val NOBODY = "nobody"
private const val ADD_ENTRY = "add-entry"
private const val UNNAMED_ALPHA = 0.45f
private const val SUMMARY_LINES = 2

// Where the faces gather: on the top row, between the space's name and the delete button.
private const val FACES_X = 0.66f
private const val FACES_Y = 0.28f

/** The rows under the header, which read as one panel. */
private enum class ItemRow { DATE, REMINDER, ADD_CHECKLIST }

/**
 * One item, the same screen for all of them: a colour block with its title and a short summary to type over, and the
 * faces of who has it. Under it, when it is due and when to remind. Then its checklist if it has one, as one list with
 * what is left first and a row at the top that adds to it, and the long details. Without a checklist the details fill
 * the screen and the header has the action that marks it done: a checklist does that by itself. An empty [itemId] is a
 * new item, with the cursor on its title: it is written once it has a name, so leaving before that leaves nothing behind.
 */
@Composable
fun ItemScreen(
    spaceId: String,
    itemId: String,
    onBack: () -> Unit,
    viewModel: ItemViewModel = koinViewModel { parametersOf(spaceId, itemId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }
    // Every way out keeps what was typed, the swipe back included.
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
                        navigationIcon = {
                            OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.item_back), onBack)
                        },
                    )
                }
            },
            list = { itemBody(state, viewModel, spaceId, itemId) },
        )
        state.scheduleEditor?.let { editor -> ScheduleEditorSheet(editor, state, viewModel) }
        state.checklistEditor?.let { editor -> ChecklistEditorSheet(editor, state, viewModel) }
        if (state.pickingMember) {
            AssigneeSheet(state, viewModel)
        }
        if (state.editingReminder) {
            ReminderSheet(state, viewModel)
        }
        if (state.confirmingDelete) {
            DeleteItemSheet(state, viewModel)
        }
    }
    if (state.askNotifications) {
        RequireNotificationPermission(onResult = { viewModel.onNotificationsAnswered() })
    }
}

/** The delete button shows only to who can delete: the rules refuse anyone else, so it has nothing to say to them. */
@Composable
private fun ItemHeader(state: ItemUiState, viewModel: ItemViewModel, onBack: () -> Unit) {
    OrbitColorHeader(
        title = state.shownTitle,
        label = state.spaceName.orEmpty(),
        backDescription = stringResource(Res.string.item_back),
        onBack = onBack,
        detail = if (state.hasEntries) {
            stringResource(Res.string.space_item_entry_count, state.checkedCount, state.entryCount).uppercase()
        } else {
            null
        },
        progress = state.entryProgress.takeIf { state.hasEntries },
        trailing = if (state.canDelete) {
            {
                OrbitIconButton(
                    icon = Res.drawable.ic_delete,
                    contentDescription = stringResource(Res.string.item_delete),
                    onClick = viewModel::openDelete,
                )
            }
        } else {
            null
        },
        onTitleChange = viewModel::onTitleChange,
        onTitleDone = viewModel::saveDrafts,
        titleDescription = stringResource(Res.string.item_name),
        titlePlaceholder = stringResource(Res.string.item_name_placeholder),
        focusTitle = state.isNew,
        summary = {
            OrbitNoteField(
                value = state.shownDescription,
                onValueChange = viewModel::onDescriptionChange,
                placeholder = stringResource(Res.string.item_description),
                onDone = viewModel::saveDrafts,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.unnamed,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
                placeholderColor = InkSoft,
                maxLines = SUMMARY_LINES,
            )
        },
        actions = if (state.hasEntries) null else ({ DoneAction(state, viewModel) }),
        floating = {
            OrbitFloatingFaces(
                faces = headerFaces(state),
                modifier = Modifier.matchParentSize().alpha(if (state.unnamed) UNNAMED_ALPHA else 1f),
                centerX = FACES_X,
                centerY = FACES_Y,
                onFaceClick = { viewModel.openMemberPicker() },
            )
        },
    )
}

/** Marks the item done. Once it is, the same place opens it again, quietly. */
@Composable
private fun DoneAction(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitHeaderAction(
        text = stringResource(if (state.isDone) Res.string.item_reopen else Res.string.item_complete),
        icon = if (state.isDone) Res.drawable.ic_undo else Res.drawable.ic_check,
        onClick = if (state.isDone) viewModel::reopen else viewModel::complete,
        enabled = state.canSubmit,
        quiet = state.isDone,
    )
}

/** Who has the item, floating over the header, or a way to add someone when nobody does. */
@Composable
private fun headerFaces(state: ItemUiState): List<OrbitFloatingFace> =
    state.assigneeFaces.map { it.floating() }.ifEmpty {
        listOf(
            OrbitFloatingFace(
                id = NOBODY,
                initials = "",
                color = Ink,
                shape = OrbitShape.Squircle,
                description = stringResource(Res.string.assignment_pick_member),
                contentColor = OrbitTheme.accent.vivid,
                icon = Res.drawable.ic_person_add,
            ),
        )
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
                item { OrbitErrorText(stringResource(Res.string.item_action_failed)) }
            }
            orbitGroup(
                items = listOfNotNull(ItemRow.DATE, ItemRow.REMINDER, ItemRow.ADD_CHECKLIST.takeUnless { state.hasChecklist }),
                key = { it.name },
            ) { row -> ItemActionRow(row, state, viewModel) }
            item(key = "gap") { Spacer(Modifier.height(OrbitSpacing.md)) }
            if (state.hasChecklist) {
                checklist(state, viewModel)
                item(key = "details") { DetailsRow(state, viewModel, orbitItem(Modifier.padding(top = OrbitSpacing.md))) }
            } else {
                item(key = "details") {
                    OrbitDetailsField(
                        value = state.shownDetails,
                        onValueChange = viewModel::onDetailsChange,
                        placeholder = stringResource(Res.string.item_details),
                        onDone = viewModel::saveDrafts,
                        modifier = orbitItem(Modifier.fillMaxWidth().alpha(if (state.unnamed) UNNAMED_ALPHA else 1f)),
                        enabled = !state.unnamed,
                    )
                }
            }
            if (itemId.isNotEmpty()) {
                item(key = "history") { ItemHistory(spaceId, itemId, orbitItem(Modifier.fillMaxWidth())) }
            }
        }
    }
}

@Composable
private fun ItemActionRow(row: ItemRow, state: ItemUiState, viewModel: ItemViewModel) {
    val modifier = Modifier.fillMaxWidth().alpha(if (state.unnamed) UNNAMED_ALPHA else 1f)
    when (row) {
        ItemRow.DATE -> OrbitActionRow(
            title = state.schedule?.let { itemScheduleLabel(it) } ?: stringResource(Res.string.item_add_date),
            icon = Res.drawable.ic_event,
            onClick = viewModel::editSchedule,
            modifier = modifier,
        )

        ItemRow.REMINDER -> OrbitActionRow(
            title = itemReminderSummary(state.reminder) ?: stringResource(Res.string.item_add_reminder),
            icon = Res.drawable.ic_notifications,
            onClick = viewModel::openReminder,
            modifier = modifier,
            shape = OrbitShape.Clover,
        )

        ItemRow.ADD_CHECKLIST -> OrbitActionRow(
            title = stringResource(Res.string.item_add_checklist),
            icon = Res.drawable.ic_list,
            onClick = viewModel::addChecklist,
            modifier = modifier,
            shape = OrbitShape.Sunny,
        )
    }
}

/** The details under a checklist: a row that grows as it is typed, so the entries stay first. */
@Composable
private fun DetailsRow(state: ItemUiState, viewModel: ItemViewModel, modifier: Modifier) {
    OrbitGroupRow(modifier.fillMaxWidth()) {
        OrbitNoteField(
            value = state.shownDetails,
            onValueChange = viewModel::onDetailsChange,
            placeholder = stringResource(Res.string.item_details),
            onDone = viewModel::saveDrafts,
            modifier = Modifier.weight(1f).padding(vertical = OrbitSpacing.sm),
        )
    }
}

/**
 * One list: the row that adds, then the entries, what is left first. An entry stays the same row wherever it is, so
 * ticking it plays out in place and the row then glides down. An empty checklist can be taken off from the row that adds.
 */
private fun LazyListScope.checklist(state: ItemUiState, viewModel: ItemViewModel) {
    orbitGroup<Any>(listOf(ADD_ENTRY) + state.shownEntries, key = { (it as? ChecklistEntry)?.id ?: ADD_ENTRY }) { row ->
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
                onRemove = if (state.hasEntries) null else viewModel::removeChecklist,
                removeDescription = stringResource(Res.string.module_checklist_remove),
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
