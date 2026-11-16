package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitAvatarSize
import com.orbit.app.core.designsystem.component.OrbitButton
import com.orbit.app.core.designsystem.component.OrbitCheck
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitFace
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.initialsOf
import com.orbit.app.feature.item.ItemFace
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.assignment_move_down
import orbit.shared.generated.resources.assignment_move_up
import orbit.shared.generated.resources.assignment_pick_member
import orbit.shared.generated.resources.assignment_rotation
import orbit.shared.generated.resources.assignment_rotation_add
import orbit.shared.generated.resources.assignment_rotation_hint
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.ic_expand_less
import orbit.shared.generated.resources.ic_expand_more
import orbit.shared.generated.resources.item_action_failed
import orbit.shared.generated.resources.module_assignment_anyone
import orbit.shared.generated.resources.or
import org.jetbrains.compose.resources.stringResource

/** A member as a face, in their colour and shape. */
@Composable
internal fun ItemFace.face() = OrbitFace(
    id = id,
    name = name,
    initials = initialsOf(name),
    color = OrbitTheme.colors.spaceAccent(index),
    shape = memberShape(index),
    photoUrl = photoUrl,
)

/**
 * Who has the item: every member is a row, and ticking one gives it to them or takes it off them. Under a divider,
 * Anyone: it belongs to nobody in particular, and ticking a member leaves it.
 */
@Composable
internal fun AssigneeSheet(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitSheet(
        title = stringResource(Res.string.assignment_pick_member),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismissMemberPicker,
    ) {
        if (state.isRotating) RotationRows(state, viewModel) else MemberRows(state, viewModel)
        OrbitLabeledDivider(stringResource(Res.string.or))
        val anyone = stringResource(Res.string.module_assignment_anyone)
        if (state.isAnyones) {
            OrbitButton(anyone, viewModel::toggleAnyone, Modifier.fillMaxWidth())
        } else {
            OrbitTonalButton(anyone, viewModel::toggleAnyone, Modifier.fillMaxWidth())
        }
        val rotation = stringResource(Res.string.assignment_rotation)
        if (state.isRotating) {
            OrbitButton(rotation, viewModel::toggleRotation, Modifier.fillMaxWidth())
        } else {
            OrbitTonalButton(rotation, viewModel::toggleRotation, Modifier.fillMaxWidth(), enabled = state.memberFaces.size >= 2)
        }
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
    }
}

/** Every member is a row, and ticking one gives the item to them or takes it off them. */
@Composable
private fun MemberRows(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitGroup(state.memberFaces, key = { it.id }, modifier = Modifier.fillMaxWidth()) { member ->
        val picked = member.id in state.assigneeIds
        OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.toggleAssignee(member.id) }) {
            val face = member.face()
            OrbitAvatar(face.initials, face.color, photoUrl = face.photoUrl, shape = face.shape, size = OrbitAvatarSize.Medium)
            Text(member.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            OrbitCheck(picked, { viewModel.toggleAssignee(member.id) }, member.name, compact = true)
        }
    }
}

/**
 * The order of the turns: tapping a member gives them the turn (ticked), and the arrows move them earlier or later.
 * The members the order does not have yet follow, and tapping one adds it at the end.
 */
@Composable
private fun RotationRows(state: ItemUiState, viewModel: ItemViewModel) {
    val turn = state.rotation?.current
    val last = state.rotationFaces.lastIndex
    Text(
        text = stringResource(Res.string.assignment_rotation_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OrbitGroup(state.rotationFaces, key = { it.id }, modifier = Modifier.fillMaxWidth()) { member ->
        val index = state.rotationFaces.indexOfFirst { it.id == member.id }
        OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.giveTurn(member.id) }) {
            val face = member.face()
            OrbitAvatar(face.initials, face.color, photoUrl = face.photoUrl, shape = face.shape, size = OrbitAvatarSize.Medium)
            Text(member.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (index > 0) {
                OrbitIconButton(
                    Res.drawable.ic_expand_less,
                    stringResource(Res.string.assignment_move_up, member.name),
                    { viewModel.moveInRotation(member.id, later = false) },
                )
            }
            if (index < last) {
                OrbitIconButton(
                    Res.drawable.ic_expand_more,
                    stringResource(Res.string.assignment_move_down, member.name),
                    { viewModel.moveInRotation(member.id, later = true) },
                )
            }
            OrbitCheck(member.id == turn, { viewModel.giveTurn(member.id) }, member.name, compact = true)
        }
    }
    if (state.outsideRotationFaces.isNotEmpty()) {
        OrbitSectionHeader(stringResource(Res.string.assignment_rotation_add), Modifier.fillMaxWidth())
        OrbitGroup(state.outsideRotationFaces, key = { it.id }, modifier = Modifier.fillMaxWidth()) { member ->
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.toggleAssignee(member.id) }) {
                val face = member.face()
                OrbitAvatar(face.initials, face.color, photoUrl = face.photoUrl, shape = face.shape, size = OrbitAvatarSize.Medium)
                Text(member.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
