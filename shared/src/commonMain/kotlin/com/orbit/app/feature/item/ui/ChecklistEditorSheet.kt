package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.feature.item.ChecklistEditor
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.checklist_edit_title
import orbit.shared.generated.resources.checklist_entry_name
import orbit.shared.generated.resources.checklist_entry_quantity
import orbit.shared.generated.resources.checklist_quantity_invalid
import orbit.shared.generated.resources.checklist_remove_entry
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.schedule_save
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ChecklistEditorSheet(editor: ChecklistEditor, state: ItemUiState, viewModel: ItemViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.checklist_edit_title),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissChecklistEditor,
    ) {
        OrbitTextField(
            value = editor.text,
            onValueChange = viewModel::onChecklistTextChange,
            placeholder = stringResource(Res.string.checklist_entry_name),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
        )
        OrbitTextField(
            value = editor.quantity,
            onValueChange = viewModel::onChecklistQuantityChange,
            placeholder = stringResource(Res.string.checklist_entry_quantity),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.submitting,
            isError = editor.quantityInvalid,
            supportingText = if (editor.quantityInvalid) stringResource(Res.string.checklist_quantity_invalid) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (state.failed) OrbitErrorText(stringResource(Res.string.item_action_failed))
        OrbitButton(
            text = stringResource(Res.string.schedule_save),
            onClick = viewModel::saveChecklistEntry,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSaveChecklist,
        )
        OrbitTextButton(
            text = stringResource(Res.string.checklist_remove_entry),
            onClick = viewModel::removeChecklistEntry,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.canSubmit,
        )
    }
}
