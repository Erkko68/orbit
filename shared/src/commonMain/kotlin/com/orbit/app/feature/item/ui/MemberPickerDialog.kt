package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitDialog
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitListItem
import com.orbit.app.core.designsystem.component.OrbitPill
import com.orbit.app.core.designsystem.component.OrbitPillTone
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.initialsOf
import com.orbit.app.domain.model.Assignment
import com.orbit.app.feature.item.ItemUiState
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.feature.item.label
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.assignment_assigned
import orbit.shared.generated.resources.assignment_pick_member
import orbit.shared.generated.resources.cancel
import orbit.shared.generated.resources.item_action_failed
import org.jetbrains.compose.resources.stringResource

/** Dialog to pick one member from the space to assign an item to. */
@Composable
internal fun MemberPickerDialog(state: ItemUiState, viewModel: ItemViewModel) {
    OrbitDialog(
        title = stringResource(Res.string.assignment_pick_member),
        dismissText = stringResource(Res.string.cancel),
        onDismiss = viewModel::dismissMemberPicker,
    ) {
        val currentAssigneeId = (state.assignment as? Assignment.ToMember)?.assigneeId
        for ((index, member) in state.members.withIndex()) {
            val isAssigned = member.userId == currentAssigneeId
            OrbitListItem(
                title = member.displayName,
                supporting = stringResource(member.role.label),
                leading = {
                    OrbitAvatar(
                        initials = initialsOf(member.displayName),
                        accent = OrbitTheme.colors.spaceAccent(index),
                        photoUrl = member.photoUrl,
                    )
                },
                trailing = if (isAssigned) {
                    {
                        OrbitPill(
                            label = stringResource(Res.string.assignment_assigned),
                            tone = OrbitPillTone.Accent,
                        )
                    }
                } else null,
                onClick = if (state.canSubmit) {
                    { viewModel.assignToMember(member.userId) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.failed) {
            OrbitErrorText(stringResource(Res.string.item_action_failed))
        }
    }
}
