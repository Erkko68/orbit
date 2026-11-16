package com.orbit.app.feature.planreview.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTaskRow
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.feature.item.ui.face
import com.orbit.app.feature.planreview.PlanReviewUiState
import com.orbit.app.feature.planreview.PlanReviewViewModel
import com.orbit.app.feature.spacehome.items.spaceItemLabel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.plan_accept_all
import orbit.shared.generated.resources.plan_accept_named
import orbit.shared.generated.resources.plan_accepted_count
import orbit.shared.generated.resources.plan_confirm
import orbit.shared.generated.resources.plan_confirm_items
import orbit.shared.generated.resources.plan_confirm_new_space
import orbit.shared.generated.resources.plan_confirm_title
import orbit.shared.generated.resources.plan_continue
import orbit.shared.generated.resources.plan_empty
import orbit.shared.generated.resources.plan_empty_body
import orbit.shared.generated.resources.plan_proposed
import orbit.shared.generated.resources.plan_save_failed
import orbit.shared.generated.resources.plan_title
import orbit.shared.generated.resources.plan_unaccept_named
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The assistant's plan, the way the space will show it once saved (#75): under the space's name, each proposal as the
 * row its item gets in Tasks. Its check accepts it, a tap opens it in the item form to edit or remove it, and Accept
 * all accepts the rest. Continue sums up what confirming creates (#76), and once it is saved [onSaved] opens the space.
 */
@Composable
fun PlanReviewScreen(
    spaceId: String,
    onBack: () -> Unit,
    onOpenProposal: (String) -> Unit,
    onSaved: (String) -> Unit,
    viewModel: PlanReviewViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedSpaceId) { state.savedSpaceId?.let(onSaved) }
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = state.spaceName ?: stringResource(Res.string.plan_title),
                navigationIcon = { OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.back), onBack) },
            )
        },
        primaryAction = if (state.empty) {
            null
        } else {
            {
                OrbitButton(
                    stringResource(Res.string.plan_continue),
                    viewModel::openConfirm,
                    Modifier.fillMaxWidth(),
                    state.canConfirm,
                )
            }
        },
        scrollable = true,
    ) {
        if (state.empty) {
            OrbitEmptyState(
                title = stringResource(Res.string.plan_empty),
                body = stringResource(Res.string.plan_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
            return@OrbitScreen
        }
        OrbitSectionHeader(
            title = stringResource(Res.string.plan_proposed),
            modifier = Modifier.fillMaxWidth(),
            count = stringResource(Res.string.plan_accepted_count, state.acceptedCount, state.proposals.size),
            action = if (state.canAcceptAll) stringResource(Res.string.plan_accept_all) else null,
            onAction = viewModel::acceptAll,
        )
        OrbitGroup(state.proposals, key = { it.item.id }, modifier = Modifier.fillMaxWidth()) { row ->
            val item = row.item
            OrbitTaskRow(
                title = item.title,
                checked = row.accepted,
                onCheckedChange = { viewModel.toggleAccepted(item.id) },
                checkDescription = stringResource(
                    if (row.accepted) Res.string.plan_unaccept_named else Res.string.plan_accept_named,
                    item.title,
                ),
                onClick = { onOpenProposal(item.id) },
                modifier = Modifier.fillMaxWidth(),
                summary = item.summary,
                label = spaceItemLabel(item, state.today)?.uppercase(),
                faces = item.faces.map { it.face() },
                overdue = item.isOverdue(state.today),
            )
        }
    }
    if (state.confirming) {
        OrbitSheet(
            title = stringResource(Res.string.plan_confirm_title),
            closeDescription = stringResource(Res.string.back),
            onDismiss = viewModel::closeConfirm,
            confirmText = stringResource(Res.string.plan_confirm),
            onConfirm = viewModel::confirm,
            confirmEnabled = state.canConfirm,
        ) {
            Text(confirmSummary(state), style = MaterialTheme.typography.bodyLarge)
            if (state.saveFailed) OrbitErrorText(stringResource(Res.string.plan_save_failed))
        }
    }
}

@Composable
private fun confirmSummary(state: PlanReviewUiState): String = pluralStringResource(
    if (state.newSpace) Res.plurals.plan_confirm_new_space else Res.plurals.plan_confirm_items,
    state.acceptedCount,
    state.spaceName.orEmpty(),
    state.acceptedCount,
)
