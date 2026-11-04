package com.orbit.app.feature.spacehome.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Expense
import com.orbit.app.domain.model.newestFirst
import com.orbit.app.domain.repository.ExpenseRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

data class SpaceExpensesUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val entries: List<Expense> = emptyList(),
    val names: Map<String, String> = emptyMap(),
)

/** The ledger loads itself, independently of the space header. Balances are added by #57. */
class SpaceExpensesViewModel(spaceId: String, expenses: ExpenseRepository, spaces: SpaceRepository) : ViewModel() {
    private val retries = Channel<Unit>(Channel.CONFLATED)
    private val loaded = combine(
        expenses.observeExpenses(spaceId),
        spaces.observeMembers(spaceId),
        spaces.observeSpaces(),
    ) { ledger, members, allSpaces ->
        val former = allSpaces.firstOrNull { it.id == spaceId }?.formerMembers.orEmpty()
        SpaceExpensesUiState(
            false,
            entries = ledger.newestFirst(),
            names = former + members.associate { it.userId to it.displayName })
    }.asLoad("Loading the ledger of $spaceId", retries)

    val uiState = loaded.map { load ->
        load.valueOrNull ?: SpaceExpensesUiState(loading = load == Load.Loading, loadFailed = load == Load.Failed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceExpensesUiState())

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }
}
