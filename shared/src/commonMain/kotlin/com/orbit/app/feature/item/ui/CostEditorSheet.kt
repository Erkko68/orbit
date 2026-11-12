package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.component.*
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** The Cost row's short editor, using the same money field and member rows as Expenses. */
@Composable
internal fun CostEditorSheet(state: ItemUiState, viewModel: ItemViewModel) {
    val editor = state.costEditor ?: return
    OrbitSheet(
        title = stringResource(Res.string.item_cost),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissCost,
        confirmText = stringResource(Res.string.account_save),
        onConfirm = viewModel::saveCost,
        confirmEnabled = state.canSaveCost,
    ) {
        OrbitMoneyField(
            value = editor.amount,
            onValueChange = viewModel::onCostAmountChange,
            label = stringResource(Res.string.expense_amount),
            placeholder = editor.placeholder,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
            isError = editor.amountInvalid,
            errorText = stringResource(Res.string.expense_amount_invalid),
            prefix = editor.currencyPrefix,
            suffix = editor.currencySuffix,
        )
        OrbitSectionHeader(stringResource(Res.string.expense_paid_by))
        OrbitGroup(state.memberFaces, key = { it.id }, modifier = Modifier.fillMaxWidth()) { member ->
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.pickCostPayer(member.id) }) {
                val face = member.face()
                OrbitAvatar(
                    face.initials,
                    face.color,
                    photoUrl = face.photoUrl,
                    shape = face.shape,
                    size = OrbitAvatarSize.Medium
                )
                Text(
                    member.name,
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                OrbitCheck(
                    editor.paidBy == member.id,
                    { viewModel.pickCostPayer(member.id) },
                    member.name,
                    compact = true,
                    enabled = !state.submitting
                )
            }
        }
        OrbitSectionHeader(stringResource(Res.string.expense_split))
        Text(stringResource(Res.string.cost_equal_hint), style = MaterialTheme.typography.bodyLarge)
        OrbitGroup(state.costParticipantFaces, key = { it.id }, modifier = Modifier.fillMaxWidth()) { member ->
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.toggleCostParticipant(member.id) }) {
                val face = member.face()
                OrbitAvatar(
                    face.initials,
                    face.color,
                    photoUrl = face.photoUrl,
                    shape = face.shape,
                    size = OrbitAvatarSize.Medium
                )
                Text(
                    member.name,
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                OrbitCheck(
                    member.id in editor.selected,
                    { viewModel.toggleCostParticipant(member.id) },
                    member.name,
                    compact = true,
                    enabled = !state.submitting
                )
            }
        }
        if (editor.selected.isEmpty() || editor.paidBy !in state.memberNames || editor.selected.any { it !in state.memberNames }) {
            OrbitErrorText(stringResource(Res.string.cost_members_invalid))
        }
        if (state.failed) OrbitErrorText(stringResource(Res.string.item_action_failed))
        if (state.cost != null) OrbitTextButton(
            stringResource(Res.string.cost_remove),
            viewModel::removeCost,
            Modifier.fillMaxWidth(),
            enabled = state.canSubmit
        )
    }
}
