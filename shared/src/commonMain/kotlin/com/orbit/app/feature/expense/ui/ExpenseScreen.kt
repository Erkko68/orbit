package com.orbit.app.feature.expense.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.*
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.formatEuro
import com.orbit.app.core.parseMoney
import com.orbit.app.feature.expense.ExpenseSplit
import com.orbit.app.feature.expense.ExpenseUiState
import com.orbit.app.feature.expense.ExpenseViewModel
import orbit.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Full-screen expense form, above the space's bar. Save is the only action that persists edits. */
@Composable
fun ExpenseScreen(
    spaceId: String,
    expenseId: String,
    onBack: () -> Unit,
    viewModel: ExpenseViewModel = koinViewModel { parametersOf(spaceId, expenseId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onBack() }
    OrbitSpaceColor(state.accent) {
        OrbitScreen(
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg),
            topBar = {
                OrbitTopBar(
                    stringResource(if (state.editing) Res.string.expense_edit else Res.string.expense_add),
                    navigationIcon = {
                        OrbitIconButton(
                            Res.drawable.ic_close,
                            stringResource(Res.string.close),
                            onBack,
                        )
                    },
                )
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

@Composable
private fun ExpenseFields(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    OrbitCard(Modifier.fillMaxWidth()) {
        OrbitHeading(
            title = stringResource(Res.string.expense_amount),
            subtitle = stringResource(Res.string.expense_amount_help),
        )
        OrbitTextField(
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            placeholder = stringResource(Res.string.expense_amount),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
            isError = state.amount.isNotEmpty() && state.cents == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Text(stringResource(Res.string.expense_title), style = MaterialTheme.typography.titleMedium)
        OrbitTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            placeholder = stringResource(Res.string.expense_title),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
        )
    }
    OrbitCard(Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.expense_paid_by), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
        ) {
            state.people.forEach { person ->
                OrbitFilterChip(
                    label = person.name,
                    selected = state.paidBy == person.id,
                    onClick = { if (!state.submitting) viewModel.selectPayer(person.id) },
                )
            }
        }
        OrbitHeading(
            title = stringResource(Res.string.expense_date),
            subtitle = stringResource(Res.string.expense_date_help),
        )
        OrbitTextField(
            value = state.date,
            onValueChange = viewModel::onDateChange,
            placeholder = stringResource(Res.string.expense_date),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
            isError = state.parsedDate == null,
            leadingIcon = Res.drawable.ic_event,
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
    ) {
        OrbitHeading(
            title = stringResource(Res.string.expense_split),
            subtitle = stringResource(
                if (state.mode == ExpenseSplit.EXACT) Res.string.expense_custom_help else Res.string.expense_equal_help,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
        ) {
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
        ExpenseParticipantsRow(state, viewModel)
        if (state.mode == ExpenseSplit.EXACT) {
            ExactSplitInputs(state, viewModel)
            ExactSplitSummary(state)
        }
    }
    if (state.failed && !state.deleteOpen) OrbitErrorText(stringResource(Res.string.expense_write_failed))
    OrbitButton(
        text = stringResource(Res.string.account_save),
        onClick = viewModel::save,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canSave,
    )
    if (state.canDelete || state.deleteOpen) {
        OrbitTextButton(
            text = stringResource(Res.string.item_delete),
            onClick = viewModel::requestDelete,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canDelete,
        )
    }
}

@Composable
private fun ExpenseParticipantsRow(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
    ) {
        state.people.forEach { person ->
            val selected = person.id in state.selected
            val shareCents = when {
                !selected -> 0L
                state.mode == ExpenseSplit.EQUAL -> state.split?.get(person.id) ?: 0L
                else -> parseMoney(state.exact[person.id].orEmpty(), state.locale) ?: 0L
            }
            Surface(
                onClick = { if (!state.submitting) viewModel.toggleMember(person.id) },
                enabled = !state.submitting,
                shape = MaterialTheme.shapes.large,
                color = if (selected) OrbitTheme.accent.done else scheme.surfaceBright,
                contentColor = scheme.onSurface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
                ) {
                    OrbitCheck(
                        checked = selected,
                        onCheckedChange = { if (!state.submitting) viewModel.toggleMember(person.id) },
                        contentDescription = person.name,
                        enabled = !state.submitting,
                        compact = true,
                    )
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatEuro(shareCents),
                        style = OrbitTheme.data.small,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExactSplitInputs(state: ExpenseUiState, viewModel: ExpenseViewModel) {
    val selectedPeople = state.people.filter { it.id in state.selected }
    if (selectedPeople.isEmpty()) return
    OrbitGroup(selectedPeople, key = { it.id }, Modifier.fillMaxWidth()) { person ->
        OrbitGroupRow {
            Column(
                modifier = Modifier.weight(1f).padding(vertical = OrbitSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            ) {
                Text(person.name, style = MaterialTheme.typography.titleMedium)
                OrbitTextField(
                    value = state.exact[person.id].orEmpty(),
                    onValueChange = { viewModel.onShareChange(person.id, it) },
                    placeholder = stringResource(Res.string.expense_share, person.name),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.submitting,
                    isError = person.id in state.invalidShareIds,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
    }
}

@Composable
private fun ExactSplitSummary(state: ExpenseUiState) {
    val progress = state.splitProgress
    OrbitCard(Modifier.fillMaxWidth()) {
        if (progress == null) {
            OrbitErrorText(stringResource(Res.string.expense_exact_help))
        } else {
            Text(
                stringResource(Res.string.expense_assigned, formatEuro(progress.assigned), formatEuro(progress.total)),
                style = OrbitTheme.data.medium,
            )
            progress.remaining?.let {
                OrbitErrorText(stringResource(Res.string.expense_remaining, formatEuro(it)))
            }
            progress.excess?.let {
                OrbitErrorText(stringResource(Res.string.expense_excess, formatEuro(it)))
            }
            if (progress.matched) {
                Text(stringResource(Res.string.expense_split_ready), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
