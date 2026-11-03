package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFacePoolHeight
import com.orbit.app.core.designsystem.component.OrbitFacePoolLargest
import com.orbit.app.core.designsystem.component.OrbitFloatingFace
import com.orbit.app.core.designsystem.component.OrbitFloatingFaces
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.initialsOf
import com.orbit.app.feature.item.ItemFace
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.assignment_anyone_note
import orbit.shared.generated.resources.assignment_pick_hint
import orbit.shared.generated.resources.assignment_pick_member
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.module_assignment_anyone
import org.jetbrains.compose.resources.stringResource

/** A member as a face that floats, in their colour and shape. */
@Composable
internal fun ItemFace.floating(selected: Boolean = false) = OrbitFloatingFace(
    id = id,
    initials = initialsOf(name),
    color = OrbitTheme.colors.spaceAccent(index),
    shape = memberShape(index),
    description = name,
    photoUrl = photoUrl,
    selected = selected,
)

/**
 * Who has the item: everyone in the space floats here, and tapping a face gives it to them or takes it off them, each tap
 * saved at once. Those who have it are bigger, with a border in the space's colour and their names underneath. Anyone is
 * the other answer: it belongs to nobody in particular, and picking a face leaves it. With nobody picked the item waits
 * for someone.
 */
@Composable
internal fun AssigneeSheet(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.assignment_pick_member),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissMemberPicker,
    ) {
        OrbitFloatingFaces(
            faces = state.memberFaces.map { it.floating(selected = it.id in state.assigneeIds) },
            modifier = Modifier.fillMaxWidth().height(OrbitFacePoolHeight),
            largest = OrbitFacePoolLargest,
            limit = state.memberFaces.size,
            onFaceClick = viewModel::toggleAssignee,
        )
        Text(
            text = assigneeNote(state),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        val anyone = stringResource(Res.string.module_assignment_anyone)
        if (state.isAnyones) {
            OrbitButton(anyone, viewModel::toggleAnyone, Modifier.fillMaxWidth())
        } else {
            OrbitTonalButton(anyone, viewModel::toggleAnyone, Modifier.fillMaxWidth())
        }
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
    }
}

/** Says who has it now, or what to do. */
@Composable
private fun assigneeNote(state: ItemUiState): String = when {
    state.isAnyones -> stringResource(Res.string.assignment_anyone_note)
    state.assigneeFaces.isEmpty() -> stringResource(Res.string.assignment_pick_hint)
    else -> state.assigneeFaces.joinToString { it.name }
}
