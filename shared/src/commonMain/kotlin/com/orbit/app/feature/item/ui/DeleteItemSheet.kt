package com.orbit.app.feature.item.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.item_delete
import orbit.shared.generated.resources.item_delete_body
import orbit.shared.generated.resources.item_delete_title
import orbit.shared.generated.resources.plan_remove
import orbit.shared.generated.resources.plan_remove_body
import orbit.shared.generated.resources.plan_remove_title
import org.jetbrains.compose.resources.stringResource

/** Asks before an item goes for everyone, or a proposal leaves the plan. */
@Composable
internal fun DeleteItemSheet(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitSheet(
        title = stringResource(if (state.proposing) Res.string.plan_remove_title else Res.string.item_delete_title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissDelete,
        confirmText = stringResource(if (state.proposing) Res.string.plan_remove else Res.string.item_delete),
        onConfirm = viewModel::deleteItem,
        confirmEnabled = !state.submitting,
        danger = true,
    ) {
        Text(
            text = stringResource(if (state.proposing) Res.string.plan_remove_body else Res.string.item_delete_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
    }
}
