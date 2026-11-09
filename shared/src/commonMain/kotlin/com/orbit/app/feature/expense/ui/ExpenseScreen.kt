package com.orbit.app.feature.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.*
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
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
 * with custom amounts. Save at the bottom is the only write.
 */
@Composable
fun ExpenseScreen(
    spaceId: String,
    expenseId: String,
    onBack: () -> Unit,
    viewModel: ExpenseViewModel = koinViewModel { parametersOf(spaceId, expenseId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onBack() }
    val form = !state.loading && !state.loadFailed && !state.missing
    OrbitSpaceColor(state.accent) {
        OrbitScreen(
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
            topBar = {
                if (form) {
                    ExpenseHeader(state, viewModel, onBack)
                } else {
                    OrbitTopBar(
                        stringResource(if (state.editing) Res.string.expense_edit else Res.string.expense_add),
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

                else -> ExpenseFields(state, viewModel)
            }
        }
        if (state.pickingPayer) PayerSheet(state, viewModel)
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
                title = stringResource(Res.string.expense_delete_title),
                closeDescription = stringResource(Res.string.close),
                onDismiss = viewModel::dismissDelete,
                confirmText = stringResource(Res.string.item_delete),
                onConfirm = viewModel::delete,
                confirmEnabled = state.canDelete,
                danger = true,
            ) {
                Text(stringResource(Res.string.expense_delete_body), style = MaterialTheme.typography.bodyLarge)
                if (state.failed) OrbitErrorText(stringResource(Res.string.expense_write_failed))
            }
        }
    }
}

/**
 * The item's colour block: the space it is in, the way back, the title typed in place with its pencil, and delete for
 * who can delete. Typing writes nothing: Save does.
 */
@Composable
private fun ExpenseHeader(state: ExpenseUiState, viewModel: ExpenseViewModel, onBack: () -> Unit) {
    OrbitColorHeader(
        title = state.title,
        label = state.spaceName,
        backDescription = stringResource(Res.string.back),
        onBack = onBack,
        trailing = if (state.canDelete) {
            { OrbitIconButton(Res.drawable.ic_delete, stringResource(Res.string.item_delete), viewModel::requestDelete) }
        } else {
            null
        },
        onTitleChange = viewModel::onTitleChange,
        titleDescription = stringResource(Res.string.expense_title),
        titlePlaceholder = stringResource(Res.string.expense_title_placeholder),
        focusTitle = !state.editing,
    )
}

/** Who paid and when, as the item form's Who and When: a labelled row whose sheet changes it. */
private enum class ExpenseRow { PAYER, DATE }

@Composable
private fun ExpenseFields(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    OrbitTextField(
        value = state.amount,
        onValueChange = viewModel::onAmountChange,
        placeholder = stringResource(Res.string.expense_amount),
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.submitting,
        isError = state.amount.isNotEmpty() && state.cents == null,
        prefix = state.currencyPrefix,
        suffix = state.currencySuffix,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
    OrbitGroup(ExpenseRow.entries, key = { it.name }, Modifier.fillMaxWidth()) { row ->
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
    SplitErrors(state)
    Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        OrbitFilterChip(
            label = stringResource(Res.string.expense_equal),
            selected = state.mode == ExpenseSplit.EQUAL,
            onClick = { viewModel.selectSplit(ExpenseSplit.EQUAL) },
        )
        OrbitFilterChip(
            label = stringResource(Res.string.expense_exact),
            selected = state.mode == ExpenseSplit.EXACT,
            onClick = { viewModel.selectSplit(ExpenseSplit.EXACT) },
        )
    }
    OrbitGroup(state.people, key = { it.id }, Modifier.fillMaxWidth()) { person ->
        SplitRow(person, state, viewModel)
    }
    if (state.failed && !state.deleteOpen) OrbitErrorText(stringResource(Res.string.expense_write_failed))
}

/** One person of the split: tap to leave them out. Their share sits at the end, or is typed there with custom amounts. */
@Composable
private fun SplitRow(person: ItemFace, state: ExpenseUiState, viewModel: ExpenseViewModel) {
    val selected = person.id in state.selected
    val toggle = { viewModel.toggleMember(person.id) }
    OrbitGroupRow(Modifier.fillMaxWidth(), onClick = if (state.submitting) null else toggle) {
        val face = person.face()
        OrbitAvatar(face.initials, face.color, photoUrl = face.photoUrl, shape = face.shape, size = OrbitAvatarSize.Medium)
        Text(
            text = person.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected && state.mode == ExpenseSplit.EXACT) {
            OrbitAmountField(
                value = state.exact[person.id].orEmpty(),
                onValueChange = { viewModel.onShareChange(person.id, it) },
                description = stringResource(Res.string.expense_share, person.name),
                placeholder = moneyInput(0, state.locale),
                enabled = !state.submitting,
                isError = person.id in state.invalidShareIds,
                prefix = state.currencyPrefix,
                suffix = state.currencySuffix,
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

/** What is wrong with typed shares: one missing or invalid, or how far they are from the total. */
@Composable
private fun SplitErrors(state: ExpenseUiState) {
    if (state.mode != ExpenseSplit.EXACT || state.selected.isEmpty()) return
    val progress = state.splitProgress
    if (progress == null) {
        OrbitErrorText(stringResource(Res.string.expense_exact_help))
        return
    }
    progress.remaining?.let { OrbitErrorText(stringResource(Res.string.expense_remaining, formatEuro(it))) }
    progress.excess?.let { OrbitErrorText(stringResource(Res.string.expense_excess, formatEuro(it))) }
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
                OrbitAvatar(face.initials, face.color, photoUrl = face.photoUrl, shape = face.shape, size = OrbitAvatarSize.Medium)
                Text(person.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OrbitCheck(person.id == state.paidBy, { viewModel.selectPayer(person.id) }, person.name, compact = true)
            }
        }
    }
}
