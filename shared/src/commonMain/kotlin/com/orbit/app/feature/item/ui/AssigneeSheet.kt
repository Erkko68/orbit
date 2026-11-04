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
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.initialsOf
import com.orbit.app.feature.item.ItemFace
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.assignment_pick_member
import orbit.shared.generated.resources.close
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
        OrbitGroup(state.memberFaces, key = { it.id }, modifier = Modifier.fillMaxWidth()) { member ->
            val picked = member.id in state.assigneeIds
            OrbitGroupRow(Modifier.fillMaxWidth(), onClick = { viewModel.toggleAssignee(member.id) }) {
                val face = member.face()
                OrbitAvatar(face.initials, face.color, photoUrl = face.photoUrl, shape = face.shape, size = OrbitAvatarSize.Medium)
                Text(member.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                OrbitCheck(picked, { viewModel.toggleAssignee(member.id) }, member.name, compact = true)
            }
        }
        OrbitLabeledDivider(stringResource(Res.string.or))
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
