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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
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
        val people = members.map { ExpensePerson(it.userId, it.displayName) } + formerIds.sorted().map {
            ExpensePerson(it, space?.formerMembers?.get(it) ?: it)
        }
        ExpenseUiState(
            loading = false,
            missing = space == null || member == null || (expenseId.isNotEmpty() && original == null),
            accent = space?.accent,
            title = original?.title.orEmpty(),
            amount = original?.let { moneyInput(it.total, locale) }.orEmpty(),
            paidBy = original?.paidBy ?: user?.id.orEmpty(),
            date = original?.date?.toString() ?: today,
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
    fun selectPayer(id: String) = edit { it.copy(paidBy = id) }
    fun onDateChange(value: String) = edit { it.copy(date = value) }
    fun selectSplit(mode: ExpenseSplit) = edit { state ->
        val exact = if (mode == ExpenseSplit.EXACT && state.mode == ExpenseSplit.EQUAL) {
            state.selected.associateWith { moneyInput(state.split?.get(it) ?: 0L, locale) }
        } else state.exact
        state.copy(mode = mode, exact = exact)
    }

    fun toggleMember(id: String) = edit {
        it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id)
    }

    fun onShareChange(id: String, value: String) = edit { it.copy(exact = it.exact + (id to value)) }
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
            val date = requireNotNull(state.parsedDate)
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
