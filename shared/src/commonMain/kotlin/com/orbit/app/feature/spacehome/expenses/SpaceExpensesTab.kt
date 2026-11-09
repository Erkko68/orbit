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
import com.orbit.app.core.designsystem.component.OrbitAmountSign
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitAvatarSize
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFaceStack
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTransferRow
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.formatDate
import com.orbit.app.core.formatEuro
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.ExpenseKind
import com.orbit.app.domain.model.MemberBalance
import com.orbit.app.domain.model.SuggestedPayment
import com.orbit.app.feature.item.ui.face
import orbit.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.absoluteValue

/**
 * Expenses, as Tricount shows them: every member's balance, the payments that would settle up the user's own, then
 * the ledger, newest first. A payment is recorded only after its confirmation sheet.
 */
@Composable
internal fun SpaceExpensesTab(
    spaceId: String,
    onOpenExpense: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpaceExpensesViewModel = koinViewModel(key = "expenses-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        when {
            state.loading -> OrbitLoadingState(stringResource(Res.string.expenses_loading), Modifier.fillMaxWidth())
            state.loadFailed -> OrbitErrorState(
                stringResource(Res.string.expenses_load_failed), stringResource(Res.string.load_failed_body),
                stringResource(Res.string.retry), viewModel::retry, Modifier.fillMaxWidth(),
            )

            else -> {
                Balances(state)
                SettleUp(state, viewModel::requestSettlement)
                Ledger(state, onOpenExpense)
            }
        }
    }
    if (state.pendingPayment != null) SettlementConfirmation(state, viewModel)
}

@Composable
private fun Balances(state: SpaceExpensesUiState) {
    OrbitSectionHeader(stringResource(Res.string.expenses_balances), Modifier.fillMaxWidth())
    OrbitGroup(state.balances.members, key = { it.memberId }, Modifier.fillMaxWidth()) { balance ->
        BalanceRow(balance, state)
    }
}

/** A balance as a sentence: the person, whether they get money back or owe it, and how much. */
@Composable
private fun BalanceRow(balance: MemberBalance, state: SpaceExpensesUiState) {
    val status = when {
        balance.cents > 0 -> Res.string.expenses_receives
        balance.cents < 0 -> Res.string.expenses_pays
        else -> Res.string.expenses_even
    }
    OrbitAmountRow(
        title = state.name(balance.memberId),
        supporting = stringResource(status),
        amount = formatEuro(balance.cents.absoluteValue),
        modifier = Modifier.fillMaxWidth(),
        sign = when {
            balance.cents > 0 -> OrbitAmountSign.Plus
            balance.cents < 0 -> OrbitAmountSign.Minus
            else -> null
        },
        leading = state.faces[balance.memberId]?.let { person ->
            {
                val face = person.face()
                OrbitAvatar(
                    face.initials,
                    face.color,
                    photoUrl = face.photoUrl,
                    shape = face.shape,
                    size = OrbitAvatarSize.Medium,
                    // A face in the row's own colour still stands out on it.
                    ring = MaterialTheme.colorScheme.surfaceBright,
                )
            }
        },
    )
}

@Composable
private fun SettleUp(state: SpaceExpensesUiState, onRequest: (SuggestedPayment) -> Unit) {
    OrbitSectionHeader(stringResource(Res.string.expenses_payments), Modifier.fillMaxWidth())
    if (state.myPayments.isEmpty()) {
        Text(
            stringResource(
                if (state.balances.payments.isEmpty()) Res.string.expenses_no_payments else Res.string.expenses_nothing_for_you,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        OrbitGroup(state.myPayments, key = { it }, Modifier.fillMaxWidth()) { payment ->
            val faces = listOfNotNull(state.faces[payment.paidBy], state.faces[payment.paidTo]).map { it.face() }
            if (payment.paidBy == state.me) {
                val sentence = stringResource(Res.string.expenses_you_owe_to, state.name(payment.paidTo))
                OrbitTransferRow(
                    faces = faces,
                    sentence = sentence,
                    amount = formatEuro(payment.cents),
                    modifier = Modifier.fillMaxWidth(),
                    action = stringResource(Res.string.expenses_pay),
                    actionIcon = Res.drawable.ic_wallet,
                    actionDescription = stringResource(Res.string.expenses_mark_paid_of, sentence),
                    onPaid = { onRequest(payment) },
                    enabled = !state.submitting,
                )
            } else {
                // Who owes it says when they paid: there is nothing for the one getting it to confirm.
                OrbitTransferRow(
                    faces = faces,
                    sentence = stringResource(Res.string.expenses_owes_you, state.name(payment.paidBy)),
                    amount = formatEuro(payment.cents),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Ledger(state: SpaceExpensesUiState, onOpenExpense: (String) -> Unit) {
    OrbitSectionHeader(stringResource(Res.string.expenses_ledger), Modifier.fillMaxWidth())
    if (state.entries.isEmpty()) {
        OrbitEmptyState(
            stringResource(Res.string.expenses_empty_title),
            stringResource(Res.string.expenses_empty_body),
            Modifier.fillMaxWidth(),
        )
    } else {
        OrbitGroup(state.entries, key = { it.id }, Modifier.fillMaxWidth()) { entry -> LedgerRow(entry, state, onOpenExpense) }
    }
}

/**
 * An expense: its title over who paid and when. A reimbursement is money moving between two people, not spending, so
 * it reads differently: both faces, "Bea paid Ana", and the tint of a ticked row. Its split is owed by the one it
 * went to.
 */
@Composable
private fun LedgerRow(entry: Expense, state: SpaceExpensesUiState, onOpenExpense: (String) -> Unit) {
    val date = formatDate(entry.date, Locale.current.toLanguageTag())
    val settlement = entry.kind == ExpenseKind.SETTLEMENT
    val payee = entry.split.keys.firstOrNull().orEmpty()
    OrbitAmountRow(
        title = if (settlement) {
            stringResource(Res.string.expenses_paid_to, state.name(entry.paidBy), state.name(payee))
        } else {
            entry.title
        },
        supporting = stringResource(
            Res.string.expenses_row_detail,
            if (settlement) entry.title else stringResource(Res.string.expense_paid_by_value, state.name(entry.paidBy)),
            date,
        ),
        amount = formatEuro(entry.total),
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenExpense(entry.id) },
        leading = if (settlement) {
            { OrbitFaceStack(listOfNotNull(state.faces[entry.paidBy], state.faces[payee]).map { it.face() }) }
        } else {
            null
        },
        done = settlement,
    )
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
    val text = stringResource(Res.string.expenses_payment, state.name(payment.paidBy), amount, state.name(payment.paidTo))
    val amountStart = text.indexOf(amount)
    val style = OrbitTheme.data.large.toSpanStyle()
    return buildAnnotatedString {
        append(text)
        if (amountStart >= 0) addStyle(style, amountStart, amountStart + amount.length)
    }
}
