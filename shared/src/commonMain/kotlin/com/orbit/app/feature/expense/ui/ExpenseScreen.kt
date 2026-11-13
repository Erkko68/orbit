package com.orbit.app.feature.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.*
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.formatDate
import com.orbit.app.core.formatEuro
import com.orbit.app.core.moneyInput
import com.orbit.app.feature.expense.ExpenseSplit
import com.orbit.app.feature.expense.ExpenseUiState
import com.orbit.app.feature.expense.ExpenseViewModel
import com.orbit.app.feature.item.ItemFace
import com.orbit.app.feature.item.ui.face
import orbit.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * An expense, with the item's colour header and its title typed in place. Then amount first, as Splitwise does, who
 * paid and when (rows that open a sheet), and everyone it is split between with their share at the end, typed there
 * as an amount, a percentage or a weight. Save at the bottom is the only write. With an [itemId] it plans that item's
 * cost (#300): the same form without a title or a date, which come from the item and the day it is done. With a
 * [draftCost] the item is not created yet: Save gives the cost to [onCostPlanned] instead of writing it.
 */
@Composable
fun ExpenseScreen(
    spaceId: String,
    expenseId: String,
    onBack: () -> Unit,
    itemId: String = "",
    draftCost: String = "",
    onOpenItem: (String) -> Unit = {},
    onCostPlanned: (String) -> Unit = {},
    sourceItemId: String = "",
    viewModel: ExpenseViewModel = koinViewModel { parametersOf(spaceId, expenseId, itemId, draftCost, sourceItemId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) {
        if (state.closed) {
            state.plannedCost?.let(onCostPlanned)
            onBack()
        }
    }
    val form = !state.loading && !state.loadFailed && !state.missing
    OrbitScreen(
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
        topBar = {
            if (form) {
                ExpenseHeader(state, viewModel, onBack)
            } else {
                OrbitTopBar(
                    stringResource(
                        when {
                            state.planning -> Res.string.item_cost
                            state.editing -> Res.string.expense_edit
                            else -> Res.string.expense_add
                        },
                    ),
                    navigationIcon = {
                        OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.back), onBack)
                    },
                )
            }
        },
        collapsingTopBar = true,
        primaryAction = if (form) {
            {
                OrbitButton(
                    text = stringResource(Res.string.account_save),
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSave,
                )
            }
        } else {
            null
        },
        scrollable = true,
    ) {
        when {
            state.loading -> OrbitLoadingState(stringResource(Res.string.expenses_loading), Modifier.fillMaxWidth())
            state.loadFailed -> OrbitErrorState(
                stringResource(Res.string.expenses_load_failed), stringResource(Res.string.load_failed_body),
                stringResource(Res.string.retry), viewModel::retry, Modifier.fillMaxWidth(),
            )

            state.missing -> OrbitEmptyState(
                stringResource(Res.string.expense_unavailable),
                stringResource(Res.string.expense_unavailable_body),
                Modifier.fillMaxWidth(),
            )

            else -> ExpenseFields(state, viewModel, onOpenItem)
        }
    }
    if (state.pickingPayer) PayerSheet(state, viewModel)
    if (state.pickingSplit) SplitSheet(state, viewModel)
    if (state.pickingDate) {
        OrbitSheet(
            title = stringResource(Res.string.schedule_pick_date),
            closeDescription = stringResource(Res.string.close),
            onDismiss = viewModel::dismissDatePicker,
        ) {
            OrbitDatePicker(state.dateMillis, stringResource(Res.string.schedule_pick), viewModel::pickDate)
        }
    }
    if (state.deleteOpen) {
        OrbitSheet(
            title = stringResource(
                if (state.planning) Res.string.cost_remove_title else Res.string.expense_delete_title,
            ),
            closeDescription = stringResource(Res.string.close),
            onDismiss = viewModel::dismissDelete,
            confirmText = stringResource(
                if (state.planning) Res.string.member_remove_confirm else Res.string.item_delete,
            ),
            onConfirm = viewModel::delete,
            confirmEnabled = state.canDelete,
            danger = true,
        ) {
            Text(
                stringResource(if (state.planning) Res.string.cost_remove_body else Res.string.expense_delete_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (state.failed) OrbitErrorText(stringResource(Res.string.expense_write_failed))
        }
    }
}

/**
 * The item's colour block: the space it is in, the way back, the title typed in place with its pencil, and delete for
 * who can delete. Typing writes nothing: Save does. A planned cost shows its item's title, and when it is charged.
 */
@Composable
private fun ExpenseHeader(state: ExpenseUiState, viewModel: ExpenseViewModel, onBack: () -> Unit) {
    OrbitColorHeader(
        title = state.title.ifEmpty { if (state.planning) stringResource(Res.string.item_cost) else "" },
        label = state.spaceName,
        backDescription = stringResource(Res.string.back),
        onBack = onBack,
        trailing = if (state.canDelete) {
            {
                OrbitIconButton(
                    Res.drawable.ic_delete,
                    stringResource(Res.string.item_delete),
                    viewModel::requestDelete
                )
            }
        } else {
            null
        },
        detail = if (state.planning) stringResource(Res.string.cost_hint) else null,
        onTitleChange = viewModel::onTitleChange.takeUnless { state.planning },
        titleDescription = stringResource(Res.string.expense_title),
        titlePlaceholder = stringResource(Res.string.expense_title_placeholder),
        focusTitle = !state.editing && !state.planning && !state.focusAmount,
    )
}

/** Who paid, when and how it is split, as the item form's Who and When: a labelled row whose sheet changes it. */
private enum class ExpenseRow { PAYER, DATE, SPLIT }

@Composable
private fun ExpenseFields(state: ExpenseUiState, viewModel: ExpenseViewModel, onOpenItem: (String) -> Unit) {
    state.sourceItemId?.let { itemId ->
        OrbitGroup(listOf(itemId), key = { it }, modifier = Modifier.fillMaxWidth()) {
            OrbitActionRow(
                title = stringResource(Res.string.expense_source_item),
                supporting = state.sourceItemTitle ?: stringResource(Res.string.expense_open_item),
                icon = Res.drawable.ic_check,
                onClick = { onOpenItem(itemId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    OrbitMoneyField(
        value = state.amount,
        onValueChange = viewModel::onAmountChange,
        label = stringResource(Res.string.expense_amount),
        placeholder = state.amountPlaceholder,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.submitting,
        isError = state.amountInvalid,
        errorText = stringResource(Res.string.expense_amount_invalid),
        prefix = state.currencyPrefix,
        suffix = state.currencySuffix,
        focusOnStart = state.focusAmount,
    )
    val rows = ExpenseRow.entries.filter { it != ExpenseRow.DATE || !state.planning }
    OrbitGroup(rows, key = { it.name }, Modifier.fillMaxWidth()) { row ->
        when (row) {
            ExpenseRow.PAYER -> OrbitActionRow(
                title = stringResource(Res.string.expense_paid_by),
                supporting = state.payer?.name,
                icon = Res.drawable.ic_wallet,
                onClick = viewModel::openPayerPicker,
                modifier = Modifier.fillMaxWidth(),
                trailing = state.payer?.let { payer -> { OrbitFaceStack(listOf(payer.face())) } },
            )

            ExpenseRow.DATE -> OrbitActionRow(
                title = stringResource(Res.string.expense_date),
                supporting = state.date?.let { formatDate(it, Locale.current.toLanguageTag()) },
                icon = Res.drawable.ic_event,
                onClick = viewModel::openDatePicker,
                modifier = Modifier.fillMaxWidth(),
                shape = OrbitShape.Sunny,
            )

            ExpenseRow.SPLIT -> OrbitActionRow(
                title = stringResource(Res.string.expense_split_mode),
                supporting = stringResource(state.mode.label),
                icon = Res.drawable.ic_tune,
                onClick = viewModel::openSplitPicker,
                modifier = Modifier.fillMaxWidth(),
                shape = OrbitShape.Clover,
            )
        }
    }
    OrbitHeading(
        title = stringResource(Res.string.expense_split),
        subtitle = state.splitTotals?.let {
            stringResource(Res.string.expense_assigned, formatEuro(it.assigned), formatEuro(it.total))
        } ?: stringResource(Res.string.expense_split_hint),
        modifier = Modifier.fillMaxWidth(),
        titleStyle = MaterialTheme.typography.titleLarge,
    )
    if (state.conversionFailed) OrbitErrorText(stringResource(Res.string.expense_conversion_failed))
    SplitErrors(state)
    OrbitGroup(state.people, key = { it.id }, Modifier.fillMaxWidth()) { person ->
        SplitRow(person, state, viewModel)
    }
    if (state.failed && !state.deleteOpen) OrbitErrorText(stringResource(Res.string.expense_write_failed))
}

private val ExpenseSplit.label
    get() = when (this) {
        ExpenseSplit.EQUAL -> Res.string.expense_equal
        ExpenseSplit.EXACT -> Res.string.expense_exact
        ExpenseSplit.PERCENTAGES -> Res.string.expense_percentages
        ExpenseSplit.WEIGHTS -> Res.string.expense_weights
    }

private val ExpenseSplit.hint
    get() = when (this) {
        ExpenseSplit.EQUAL -> Res.string.expense_equal_hint
        ExpenseSplit.EXACT -> Res.string.expense_exact_hint
        ExpenseSplit.PERCENTAGES -> Res.string.expense_percentages_hint
        ExpenseSplit.WEIGHTS -> Res.string.expense_weights_hint
    }

/**
 * One person of the split: tap to leave them out. Their share sits at the end, or is typed there. A percentage or a
 * weight shows the amount it comes to under the name. An automatic custom share is the greyed amount in an empty
 * field; typing fixes it, and the undo icon in a fixed one makes it automatic again.
 */
@Composable
private fun SplitRow(person: ItemFace, state: ExpenseUiState, viewModel: ExpenseViewModel) {
    val selected = person.id in state.selected
    val typed = selected && state.mode != ExpenseSplit.EQUAL
    val toggle = { viewModel.toggleMember(person.id) }
    OrbitGroupRow(Modifier.fillMaxWidth(), onClick = if (state.submitting) null else toggle) {
        val face = person.face()
        OrbitAvatar(
            face.initials,
            face.color,
            photoUrl = face.photoUrl,
            shape = face.shape,
            size = OrbitAvatarSize.Medium
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (typed && state.mode != ExpenseSplit.EXACT) {
                Text(
                    text = if (state.split == null) {
                        stringResource(Res.string.expense_share_unavailable)
                    } else {
                        formatEuro(state.shares[person.id] ?: 0L)
                    },
                    style = OrbitTheme.data.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (typed) {
            val weight = state.mode == ExpenseSplit.WEIGHTS
            val automatic = state.custom && person.id in state.automatic
            OrbitAmountField(
                value = state.shareInputs[person.id].orEmpty(),
                onValueChange = { viewModel.onShareChange(person.id, it) },
                description = stringResource(
                    when {
                        weight -> Res.string.expense_weight_share
                        automatic -> Res.string.expense_automatic_share
                        else -> Res.string.expense_share
                    },
                    person.name,
                ),
                placeholder = when {
                    automatic -> state.automaticInputs[person.id]
                        ?: stringResource(Res.string.expense_share_unavailable)

                    weight -> "0"
                    else -> moneyInput(0, state.locale)
                },
                enabled = !state.submitting,
                isError = person.id in state.invalidShareIds,
                prefix = state.currencyPrefix.takeIf { state.mode == ExpenseSplit.EXACT },
                suffix = when (state.mode) {
                    ExpenseSplit.EXACT -> state.currencySuffix
                    ExpenseSplit.PERCENTAGES -> stringResource(Res.string.expense_percent_sign)
                    else -> null
                },
                onReset = if (state.custom && !automatic) ({ viewModel.makeAutomatic(person.id) }) else null,
                resetDescription = stringResource(Res.string.expense_make_automatic_person, person.name),
            )
        } else {
            Text(
                text = formatEuro(state.shares[person.id] ?: 0L),
                style = OrbitTheme.data.medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
            )
        }
        OrbitCheck(selected, { toggle() }, person.name, enabled = !state.submitting, compact = true)
    }
}

/** What is wrong with typed shares: one missing or invalid, or how far they are from the total or from 100 %. */
@Composable
private fun SplitErrors(state: ExpenseUiState) {
    if (state.mode == ExpenseSplit.EQUAL || state.selected.isEmpty()) return
    val progress = state.splitProgress
    if (state.cents == null || state.invalidShareIds.isNotEmpty() || progress == null && state.mode != ExpenseSplit.WEIGHTS) {
        OrbitErrorText(stringResource(Res.string.expense_exact_help))
        return
    }
    if (progress == null) return
    val format: @Composable (Long) -> String = if (state.mode == ExpenseSplit.PERCENTAGES) {
        { stringResource(Res.string.expense_percent, moneyInput(it, state.locale)) }
    } else {
        { formatEuro(it) }
    }
    progress.remaining?.let { OrbitErrorText(stringResource(Res.string.expense_remaining, format(it))) }
    progress.excess?.let { OrbitErrorText(stringResource(Res.string.expense_excess, format(it))) }
}

/** How to split: every mode is a row with what it means, and tapping one picks it and closes the sheet. */
@Composable
private fun SplitSheet(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.expense_split_mode),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissSplitPicker,
    ) {
        OrbitGroup(ExpenseSplit.entries, key = { it.name }, Modifier.fillMaxWidth()) { mode ->
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.selectSplit(mode) }) {
                OrbitHeading(stringResource(mode.label), stringResource(mode.hint), Modifier.weight(1f))
                OrbitCheck(
                    mode == state.mode,
                    { viewModel.selectSplit(mode) },
                    stringResource(mode.label),
                    compact = true
                )
            }
        }
    }
}

/** Who paid: every person is a row, and tapping one picks them and closes the sheet. */
@Composable
private fun PayerSheet(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.expense_paid_by),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissPayerPicker,
    ) {
        OrbitGroup(state.people, key = { it.id }, Modifier.fillMaxWidth()) { person ->
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.selectPayer(person.id) }) {
                val face = person.face()
                OrbitAvatar(
                    face.initials,
                    face.color,
                    photoUrl = face.photoUrl,
                    shape = face.shape,
                    size = OrbitAvatarSize.Medium
                )
                Text(person.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OrbitCheck(person.id == state.paidBy, { viewModel.selectPayer(person.id) }, person.name, compact = true)
            }
        }
    }
}
