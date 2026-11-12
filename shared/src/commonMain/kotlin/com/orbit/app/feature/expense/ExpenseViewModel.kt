package com.orbit.app.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.deviceMoneyLocale
import com.orbit.app.core.isMoneyInput
import com.orbit.app.core.moneyInput
import com.orbit.app.core.valueOrNull
import com.orbit.app.core.orOnFailure
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.NewExpense
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ExpenseRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.domain.usecase.HUNDRED_PERCENT
import com.orbit.app.domain.usecase.splitByWeights
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onStart
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
    items: ItemRepository,
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
        items.observeItems(spaceId).onStart { emit(emptyList()) }
            .orOnFailure("Loading the expense's source item", emptyList()),
    ) { allSpaces, members, ledger, user, sourceItems ->
        val space = allSpaces.firstOrNull { it.id == spaceId }
        val original = ledger.firstOrNull { it.id == expenseId }
        val member = members.firstOrNull { it.userId == user?.id }
        val currentIds = members.map { it.userId }.toSet()
        val knownIds = currentIds + space?.formerMembers.orEmpty().keys + original?.split.orEmpty().keys
        val allocation = original?.customAllocation?.takeIf { it.keys.all { id -> id in knownIds } }
        val formerIds = (original?.let { it.split.keys + it.paidBy }.orEmpty() + allocation.orEmpty().keys) - currentIds
        val people = members.mapIndexed { index, it -> ItemFace(it.userId, it.displayName, index, it.photoUrl) } +
                formerIds.sorted().mapIndexed { index, it ->
                    ItemFace(it, space?.formerMembers?.get(it) ?: it, members.size + index, null)
                }
        ExpenseUiState(
            loading = false,
            missing = space == null || member == null || (expenseId.isNotEmpty() && original == null),
            spaceName = space?.name.orEmpty(),
            title = original?.title.orEmpty(),
            amount = original?.let { moneyInput(it.total, locale) }.orEmpty(),
            paidBy = original?.paidBy ?: user?.id.orEmpty(),
            date = original?.date ?: today,
            // Stored shares are authoritative: opening an edit never splits or rounds them again.
            mode = if (original == null) ExpenseSplit.EQUAL else ExpenseSplit.EXACT,
            selected = allocation?.keys ?: original?.split?.keys ?: currentIds,
            exact = (allocation?.keys ?: original?.split?.keys).orEmpty().associateWith {
                moneyInput(original?.split?.get(it) ?: 0L, locale)
            },
            automatic = allocation.orEmpty().filterValues { it }.keys,
            people = people,
            locale = locale,
            original = original,
            sourceItemTitle = sourceItems.firstOrNull { it.id == original?.itemId }?.title,
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
            spaceName = context?.spaceName ?: state.spaceName,
            people = context?.people ?: state.people,
            original = context?.original ?: state.original,
            sourceItemTitle = context?.sourceItemTitle,
            member = context?.member,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseUiState())

    private fun edit(change: (ExpenseUiState) -> ExpenseUiState) {
        val current = uiState.value
        if (!current.submitting && !current.loading && !current.loadFailed && !current.closed) {
            form.value = change(current.copy(conversionFailed = false)).copy(failed = false)
        }
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value) }

    /** Amounts, percentages and weights take only what they can hold: a letter or a sign never reaches the state. */
    fun onAmountChange(value: String) = edit {
        if (isMoneyInput(value) && value != it.amount) it.copy(amount = value, allocationChanged = true) else it
    }

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
     * Custom amounts and percentages start from the split on screen (#55's simple conversion, so a round trip may move
     * a cent), or all automatic coming from Equal. Who is automatic stays between the two; from Weights nobody is.
     * Drafts of people left out are dropped. A split that is not valid yet cannot be converted: it stays, with why.
     */
    fun selectSplit(mode: ExpenseSplit) = edit { state ->
        if (mode == state.mode) return@edit state.copy(pickingSplit = false)
        val next = state.copy(
            mode = mode,
            exact = emptyMap(),
            percentages = emptyMap(),
            automatic = emptySet(),
            allocationChanged = true,
            pickingSplit = false,
        )
        if (!next.custom) return@edit next
        if (state.mode == ExpenseSplit.EQUAL) return@edit next.copy(automatic = state.selected)
        val split = state.split
        val values =
            (if (mode == ExpenseSplit.PERCENTAGES) split?.let { splitByWeights(HUNDRED_PERCENT, it) } else split)
                ?: return@edit state.copy(conversionFailed = true, pickingSplit = false)
        val inputs = state.selected.associateWith { moneyInput(values[it] ?: 0L, state.locale) }
        val automatic = if (state.custom) state.automatic else emptySet()
        if (mode == ExpenseSplit.EXACT) next.copy(exact = inputs, automatic = automatic)
        else next.copy(percentages = inputs, automatic = automatic)
    }

    /** Someone added back keeps their draft and status; someone new to a custom split starts automatic. */
    fun toggleMember(id: String) = edit {
        if (it.people.none { person -> person.id == id }) return@edit it
        if (id in it.selected) return@edit it.copy(selected = it.selected - id, allocationChanged = true)
        val known = id in it.exact || id in it.percentages || id in it.automatic
        it.copy(
            selected = it.selected + id,
            automatic = if (it.custom && !known) it.automatic + id else it.automatic,
            allocationChanged = true,
        )
    }

    fun onShareChange(id: String, value: String) = edit {
        val allowed = if (it.mode == ExpenseSplit.WEIGHTS) value.all { c -> c in '0'..'9' } else isMoneyInput(value)
        if (!allowed || id !in it.selected || value == it.shareInputs[id]) return@edit it
        val state = it.copy(automatic = it.automatic - id, allocationChanged = true)
        when (it.mode) {
            ExpenseSplit.EQUAL -> it
            ExpenseSplit.EXACT -> state.copy(exact = it.exact + (id to value))
            ExpenseSplit.PERCENTAGES -> state.copy(percentages = it.percentages + (id to value))
            ExpenseSplit.WEIGHTS -> state.copy(weights = it.weights + (id to value))
        }
    }

    fun makeAutomatic(id: String) = edit {
        if (it.custom && id in it.selected) it.copy(automatic = it.automatic + id, allocationChanged = true) else it
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
                expenses.add(
                    spaceId, NewExpense(
                        state.title.trim(), state.paidBy, split, date,
                        customAllocation = state.customAllocation
                    )
                )
            } else {
                expenses.edit(spaceId, expenseId, state.title.trim(), state.paidBy, split, date, state.customAllocation)
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
