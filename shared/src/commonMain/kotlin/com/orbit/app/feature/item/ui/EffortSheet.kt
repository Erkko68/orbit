package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitCheck
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.effort_title
import orbit.shared.generated.resources.effort_points
import orbit.shared.generated.resources.effort_remove
import orbit.shared.generated.resources.effort_body
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.space_settings_save
import org.jetbrains.compose.resources.stringResource

/** The optional effort module, edited in the same sheet and row style as the item's other fields. */
@Composable
internal fun EffortSheet(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.effort_title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissEffort,
        confirmText = stringResource(Res.string.space_settings_save),
        onConfirm = viewModel::saveEffort,
        confirmEnabled = state.canSaveEffort,
    ) {
        Text(stringResource(Res.string.effort_body), style = MaterialTheme.typography.bodyMedium)
        OrbitGroup((1..5).toList(), key = { it }, modifier = Modifier.fillMaxWidth()) { points ->
            val label = stringResource(Res.string.effort_points, points)
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.selectEffort(points) }) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OrbitCheck(
                    !state.removingEffort && state.effortDraft == points,
                    { viewModel.selectEffort(points) }, label, compact = true,
                )
            }
        }
        if (state.effort != null) {
            OrbitTonalButton(
                stringResource(Res.string.effort_remove), viewModel::clearEffort,
                Modifier.fillMaxWidth(), enabled = state.canSubmit && !state.removingEffort,
            )
        }
        if (state.failed) OrbitErrorText(stringResource(Res.string.item_action_failed))
    }
}
