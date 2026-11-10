package com.orbit.app.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.deviceMoneyLocale
import com.orbit.app.core.moneyInput
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ExpenseRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.domain.usecase.HUNDRED_PERCENT
import com.orbit.app.domain.usecase.splitByWeights
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import com.orbit.app.feature.item.ItemFace
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Instant
import kotlin.time.Clock

/** One explicit Save writes a new entry or edits the existing one. Nothing is persisted while typing. */
class ExpenseViewModel(
    private val spaceId: String,
    private val expenseId: String,
    private val expenses: ExpenseRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val retries = Channel<Unit>(Channel.CONFLATED)
    private val form = MutableStateFlow<ExpenseUiState?>(null)
    private val locale = deviceMoneyLocale()
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val loaded = combine(
        spaces.observeSpaces(),
        spaces.observeMembers(spaceId),
        if (expenseId.isEmpty()) flowOf(emptyList<Expense>()) else expenses.observeExpenses(spaceId),
        auth.session,
    ) { allSpaces, members, ledger, user ->
        val space = allSpaces.firstOrNull { it.id == spaceId }
        val original = ledger.firstOrNull { it.id == expenseId }
        val member = members.firstOrNull { it.userId == user?.id }
        val formerIds = original?.let { it.split.keys + it.paidBy }.orEmpty() - members.map { it.userId }.toSet()
        val people = members.mapIndexed { index, it -> ItemFace(it.userId, it.displayName, index, it.photoUrl) } +
            formerIds.sorted().mapIndexed { index, it ->
                ItemFace(it, space?.formerMembers?.get(it) ?: it, members.size + index, null)
            }
        ExpenseUiState(
            loading = false,
            missing = space == null || member == null || (expenseId.isNotEmpty() && original == null),
            accent = space?.accent,
            spaceName = space?.name.orEmpty(),
            title = original?.title.orEmpty(),
            amount = original?.let { moneyInput(it.total, locale) }.orEmpty(),
            paidBy = original?.paidBy ?: user?.id.orEmpty(),
            date = original?.date ?: today,
            // Stored shares are authoritative: opening an edit never splits or rounds them again.
            mode = if (original == null) ExpenseSplit.EQUAL else ExpenseSplit.EXACT,
            selected = original?.split?.keys ?: members.map { it.userId }.toSet(),
            exact = original?.split?.mapValues { moneyInput(it.value, locale) }.orEmpty(),
            people = people,
            locale = locale,
            original = original,
            member = member,
        )
    }.asLoad("Loading expense $expenseId in $spaceId", retries)

    val uiState = combine(loaded, form) { load, edits ->
        val context = load.valueOrNull
        val state = edits ?: context ?: ExpenseUiState(locale = locale)
        state.copy(
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            missing = context?.missing ?: false,
            accent = context?.accent ?: state.accent,
            spaceName = context?.spaceName ?: state.spaceName,
            people = context?.people ?: state.people,
            original = context?.original ?: state.original,
            member = context?.member,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseUiState())

    private fun edit(change: (ExpenseUiState) -> ExpenseUiState) {
        val current = uiState.value
        if (!current.submitting && !current.loading && !current.loadFailed && !current.closed) {
            form.value = change(current).copy(failed = false)
        }
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }
    fun onAmountChange(value: String) = edit { it.copy(amount = value) }
    fun openPayerPicker() = edit { it.copy(pickingPayer = true) }
    fun dismissPayerPicker() = edit { it.copy(pickingPayer = false) }
    fun selectPayer(id: String) = edit { it.copy(paidBy = id, pickingPayer = false) }
    fun openSplitPicker() = edit { it.copy(pickingSplit = true) }
    fun dismissSplitPicker() = edit { it.copy(pickingSplit = false) }
    fun openDatePicker() = edit { it.copy(pickingDate = true) }
    fun dismissDatePicker() = edit { it.copy(pickingDate = false) }

    /** The calendar gives the day as UTC midnight. */
    fun pickDate(utcMillis: Long) = edit {
        it.copy(date = Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date, pickingDate = false)
    }
    /**
     * Custom amounts and percentages start from the split on screen, so switching keeps what the user sees: €40 as
     * 20 / 10 / 10 becomes 50 / 25 / 25 %. Without a split yet, percentages start equal. Weights keep what was typed.
     * Picking one closes the sheet.
     */
    fun selectSplit(mode: ExpenseSplit) = edit { state ->
        if (mode == state.mode) return@edit state.copy(pickingSplit = false)
        val split = state.split
        when (mode) {
            ExpenseSplit.EXACT -> split?.let { state.copy(exact = state.inputs(it)) } ?: state
            ExpenseSplit.PERCENTAGES -> state.copy(
                percentages = state.inputs(
                    splitByWeights(HUNDRED_PERCENT, split ?: state.selected.associateWith { 1L }).orEmpty(),
                ),
            )
            else -> state
        }.copy(mode = mode, pickingSplit = false)
    }

    private fun ExpenseUiState.inputs(values: Map<String, Long>) =
        selected.associateWith { moneyInput(values[it] ?: 0L, locale) }

    fun toggleMember(id: String) = edit {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    fun onShareChange(id: String, value: String) = edit {
        when (it.mode) {
            ExpenseSplit.EQUAL -> it
            ExpenseSplit.EXACT -> it.copy(exact = it.exact + (id to value))
            ExpenseSplit.PERCENTAGES -> it.copy(percentages = it.percentages + (id to value))
            ExpenseSplit.WEIGHTS -> it.copy(weights = it.weights + (id to value))
        }
    }
    fun requestDelete() {
        if (uiState.value.canDelete) edit { it.copy(deleteOpen = true) }
    }

    fun dismissDelete() = edit { it.copy(deleteOpen = false) }
    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    fun save() {
        val state = uiState.value
        if (!state.canSave || form.value?.submitting == true) return
        val split = state.split ?: return
        submit(state, "Saving expense in $spaceId") {
            val date = requireNotNull(state.date)
            if (expenseId.isEmpty()) {
                expenses.add(spaceId, NewExpense(state.title.trim(), state.paidBy, split, date))
            } else {
                expenses.edit(spaceId, expenseId, state.title.trim(), state.paidBy, split, date)
            }
        }
    }

    fun delete() {
        val state = uiState.value
        if (!state.canDelete || !state.deleteOpen || form.value?.submitting == true) return
        submit(state, "Deleting expense $expenseId") { expenses.delete(spaceId, expenseId) }
    }

    private fun submit(state: ExpenseUiState, description: String, write: suspend () -> Unit) {
        form.value = state.copy(submitting = true, failed = false)
        viewModelScope.launch {
            val result = attempt(description, write)
            form.value = requireNotNull(form.value).copy(
                submitting = false,
                failed = result.isFailure,
                closed = result.isSuccess
            )
        }
    }
}
