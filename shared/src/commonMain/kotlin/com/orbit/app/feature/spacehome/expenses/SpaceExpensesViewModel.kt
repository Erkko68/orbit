package com.orbit.app.feature.spacehome.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Balances
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.ExpenseKind
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.model.SuggestedPayment
import com.orbit.app.domain.model.newestFirst
import com.orbit.app.domain.model.validateExpense
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ExpenseRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.domain.usecase.calculateBalances
import com.orbit.app.feature.item.ItemFace
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.expenses_settlement_title
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

data class SpaceExpensesUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val entries: List<Expense> = emptyList(),
    /** Members, and former members by the name they left with, in their colour and shape. */
    val faces: Map<String, ItemFace> = emptyMap(),
    /** The signed-in user, whose payments Settle up shows. */
    val me: String? = null,
    val balances: Balances = Balances(emptyList(), emptyList()),
    val pendingPayment: SuggestedPayment? = null,
    val submitting: Boolean = false,
    val failed: Boolean = false,
) {
    fun name(id: String): String = faces[id]?.name ?: id

    /** The payments the user is in, paying or getting paid: the only ones Settle up shows. */
    val myPayments: List<SuggestedPayment>
        get() = balances.payments.filter { it.paidBy == me || it.paidTo == me }

    val paymentChanged: Boolean
        get() = pendingPayment != null && pendingPayment !in balances.payments && !loading && !loadFailed && !submitting

    val canConfirm: Boolean
        get() = pendingPayment != null && pendingPayment.paidBy == me && pendingPayment in balances.payments && !loading && !loadFailed && !submitting
}

private data class SettlementState(
    val payment: SuggestedPayment? = null,
    val submitting: Boolean = false,
    val failed: Boolean = false,
)

/**
 * Balances follow the ledger. A payment is recorded only by who pays it, after an explicit confirmation: the one who
 * owes is trusted to say they paid, and nobody confirms receiving it (the rules hold the same).
 */
class SpaceExpensesViewModel(
    private val spaceId: String,
    private val expenses: ExpenseRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val retries = Channel<Unit>(Channel.CONFLATED)
    private val settlement = MutableStateFlow(SettlementState())
    private val loaded = combine(
        expenses.observeExpenses(spaceId),
        spaces.observeMembers(spaceId),
        spaces.observeSpaces(),
        auth.session,
    ) { ledger, members, allSpaces, user ->
        val former = allSpaces.firstOrNull { it.id == spaceId }?.formerMembers.orEmpty()
        val current = members.mapIndexed { index, it -> ItemFace(it.userId, it.displayName, index, it.photoUrl) }
        val left = (former.keys - current.map { it.id }.toSet()).sorted().mapIndexed { index, id ->
            ItemFace(id, former.getValue(id), members.size + index, null)
        }
        SpaceExpensesUiState(
            loading = false,
            entries = ledger.newestFirst(),
            faces = (current + left).associateBy { it.id },
            me = user?.id,
            balances = calculateBalances(ledger, members.map { it.userId }),
        )
    }.asLoad("Loading the ledger of $spaceId", retries)

    val uiState = combine(loaded, settlement) { load, selected ->
        val state =
            load.valueOrNull ?: SpaceExpensesUiState(loading = load == Load.Loading, loadFailed = load == Load.Failed)
        state.copy(pendingPayment = selected.payment, submitting = selected.submitting, failed = selected.failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceExpensesUiState())

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    fun requestSettlement(payment: SuggestedPayment) {
        val state = uiState.value
        if (!state.loading && !state.loadFailed && !settlement.value.submitting && payment.paidBy == state.me &&
            payment in state.balances.payments
        ) {
            settlement.value = SettlementState(payment)
        }
    }

    fun dismissSettlement() {
        if (!settlement.value.submitting) settlement.value = SettlementState()
    }

    fun confirmSettlement() {
        val state = uiState.value
        if (!state.canConfirm || settlement.value.submitting) return
        val payment = state.pendingPayment ?: return
        if (settlement.value.payment != payment) return
        settlement.value = SettlementState(payment, submitting = true)
        viewModelScope.launch {
            val result = attempt("Recording a settlement in $spaceId") {
                val title = getString(Res.string.expenses_settlement_title)
                val split = mapOf(payment.paidTo to payment.cents)
                val people = state.balances.members.map { it.memberId }
                check(validateExpense(title, payment.paidBy, split, payment.cents, people).isEmpty())
                expenses.add(
                    spaceId,
                    NewExpense(
                        title,
                        payment.paidBy,
                        split,
                        Clock.System.todayIn(TimeZone.currentSystemDefault()),
                        ExpenseKind.SETTLEMENT
                    ),
                )
            }
            settlement.value = if (result.isSuccess) SettlementState() else SettlementState(payment, failed = true)
        }
    }
}
