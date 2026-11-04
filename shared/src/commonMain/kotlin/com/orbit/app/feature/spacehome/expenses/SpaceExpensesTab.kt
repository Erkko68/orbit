package com.orbit.app.feature.spacehome.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAmountRow
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.formatDate
import com.orbit.app.core.formatEuro
import orbit.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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

            state.entries.isEmpty() -> OrbitEmptyState(
                stringResource(Res.string.expenses_empty_title),
                stringResource(Res.string.expenses_empty_body),
                Modifier.fillMaxWidth(),
            )

            else -> OrbitGroup(state.entries, key = { it.id }, Modifier.fillMaxWidth()) { entry ->
                val payer = state.names[entry.paidBy] ?: entry.paidBy
                val formattedDate = formatDate(entry.date, Locale.current.toLanguageTag())
                OrbitAmountRow(
                    title = entry.title,
                    supporting = stringResource(Res.string.expense_paid_by_value, payer),
                    amount = stringResource(Res.string.expense_total_value, formatEuro(entry.total)),
                    date = stringResource(Res.string.expense_date_value, formattedDate),
                    onClick = { onOpenExpense(entry.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
