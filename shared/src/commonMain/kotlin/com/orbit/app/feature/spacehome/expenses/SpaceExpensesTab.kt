package com.orbit.app.feature.spacehome.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAmountRow
import com.orbit.app.core.designsystem.component.OrbitBalanceRow
import com.orbit.app.core.designsystem.component.OrbitTransferRow
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.formatDate
import com.orbit.app.core.formatEuro
import com.orbit.app.domain.model.SuggestedPayment
import orbit.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue

@Composable
internal fun SpaceExpensesTab(
    spaceId: String,
    onOpenExpense: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceExpensesViewModel = koinViewModel(key = "expenses-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
        when {
            state.loading -> OrbitLoadingState(stringResource(Res.string.expenses_loading), Modifier.fillMaxWidth())
            state.loadFailed -> OrbitErrorState(
                stringResource(Res.string.expenses_load_failed), stringResource(Res.string.load_failed_body),
                stringResource(Res.string.retry), viewModel::retry, Modifier.fillMaxWidth(),
            )

            else -> {
                ExpenseBalances(state)
                SuggestedPayments(state, viewModel::requestSettlement)
                ExpenseLedger(state, onOpenExpense)
            }
        }
    }
    if (state.pendingPayment != null) SettlementConfirmation(state, viewModel)
}

@Composable
private fun ExpenseBalances(state: SpaceExpensesUiState) {
    OrbitSectionHeader(stringResource(Res.string.expenses_balances), Modifier.fillMaxWidth(), centered = true)
    OrbitGroup(state.balances.members, key = { it.memberId }, Modifier.fillMaxWidth()) { balance ->
        val name = state.names[balance.memberId] ?: balance.memberId
        val label = when {
            balance.cents > 0 -> Res.string.expenses_receives
            balance.cents < 0 -> Res.string.expenses_pays
            else -> Res.string.expenses_even
        }
        val direction = when {
            balance.cents > 0 -> Res.drawable.ic_arrow_back
            balance.cents < 0 -> Res.drawable.ic_arrow_forward
            else -> null
        }
        OrbitBalanceRow(
            name = name,
            status = stringResource(label),
            // Long.MIN_VALUE keeps its sign rather than overflowing into an incorrect positive amount.
            amount = formatEuro(balance.cents.absoluteValue),
            direction = direction,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SuggestedPayments(state: SpaceExpensesUiState, onRequest: (SuggestedPayment) -> Unit) {
    OrbitSectionHeader(stringResource(Res.string.expenses_payments), Modifier.fillMaxWidth(), centered = true)
    if (state.balances.payments.isEmpty()) {
        Text(stringResource(Res.string.expenses_no_payments), style = MaterialTheme.typography.bodyLarge)
    } else {
        OrbitGroup(state.balances.payments, key = { it }, Modifier.fillMaxWidth()) { payment ->
            OrbitTransferRow(
                payer = state.names[payment.paidBy] ?: payment.paidBy,
                payee = state.names[payment.paidTo] ?: payment.paidTo,
                payerLabel = stringResource(Res.string.expenses_who_pays),
                payeeLabel = stringResource(Res.string.expenses_who_receives),
                amount = formatEuro(payment.cents),
                action = stringResource(Res.string.expenses_mark_paid),
                onAction = { onRequest(payment) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.submitting,
            )
        }
    }
}

@Composable
private fun ExpenseLedger(state: SpaceExpensesUiState, onOpenExpense: (String) -> Unit) {
    OrbitSectionHeader(stringResource(Res.string.expenses_ledger), Modifier.fillMaxWidth(), centered = true)
    if (state.entries.isEmpty()) {
        OrbitEmptyState(
            stringResource(Res.string.expenses_empty_title),
            stringResource(Res.string.expenses_empty_body),
            Modifier.fillMaxWidth(),
        )
    } else OrbitGroup(state.entries, key = { it.id }, Modifier.fillMaxWidth()) { entry ->
        val payer = state.names[entry.paidBy] ?: entry.paidBy
        val formattedDate = formatDate(entry.date, Locale.current.toLanguageTag())
        OrbitAmountRow(
            title = entry.title,
            supporting = stringResource(Res.string.expense_paid_by_value, payer),
            amount = formatEuro(entry.total),
            date = formattedDate,
            onClick = { onOpenExpense(entry.id) },
            modifier = Modifier.fillMaxWidth(),
            supportingEmphasis = payer,
        )
    }
}

@Composable
private fun SettlementConfirmation(state: SpaceExpensesUiState, viewModel: SpaceExpensesViewModel) {
    val payment = state.pendingPayment ?: return
    OrbitSheet(
        title = stringResource(Res.string.expenses_confirm_title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissSettlement,
        confirmText = stringResource(if (state.submitting) Res.string.expenses_recording else Res.string.expenses_mark_paid),
        confirmEnabled = state.canConfirm,
        onConfirm = viewModel::confirmSettlement,
    ) {
        Text(paymentLabel(payment, state), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(Res.string.expenses_confirm_body), style = MaterialTheme.typography.bodyMedium)
        if (state.failed) OrbitErrorText(stringResource(Res.string.expense_write_failed))
        if (state.paymentChanged) OrbitErrorText(stringResource(Res.string.expenses_payment_changed))
        if (state.loadFailed) {
            OrbitErrorText(stringResource(Res.string.expenses_load_failed))
            OrbitTextButton(stringResource(Res.string.retry), viewModel::retry)
        }
    }
}

/** Keeps the translated sentence together, with only its exact monetary value in the data typeface. */
@Composable
private fun paymentLabel(payment: SuggestedPayment, state: SpaceExpensesUiState): AnnotatedString {
    val amount = formatEuro(payment.cents)
    val text = stringResource(
        Res.string.expenses_payment,
        state.names[payment.paidBy] ?: payment.paidBy,
        amount,
        state.names[payment.paidTo] ?: payment.paidTo,
    )
    val amountStart = text.indexOf(amount)
    val style = OrbitTheme.data.large.toSpanStyle()
    return buildAnnotatedString {
        append(text)
        if (amountStart >= 0) addStyle(style, amountStart, amountStart + amount.length)
    }
}
