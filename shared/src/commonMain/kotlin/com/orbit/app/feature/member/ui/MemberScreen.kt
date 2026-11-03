package com.orbit.app.feature.member.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitColorHeader
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitFloatingFace
import com.orbit.app.core.designsystem.component.OrbitFloatingFaces
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitHeaderAction
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.component.orbitGroup
import com.orbit.app.core.designsystem.theme.OrbitSpaceColor
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.item.ui.itemDateLabel
import com.orbit.app.feature.item.ui.itemDueLabel
import com.orbit.app.feature.member.MemberUiState
import com.orbit.app.feature.member.MemberViewModel
import com.orbit.app.feature.spacehome.members.RolePickerPage
import com.orbit.app.feature.spacehome.members.SpaceMember
import com.orbit.app.feature.spacehome.members.SpaceMembersViewModel
import com.orbit.app.feature.spacehome.members.TransferDialog
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_expand_more
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.member_empty_body
import orbit.shared.generated.resources.member_empty_title
import orbit.shared.generated.resources.member_has
import orbit.shared.generated.resources.member_lately
import orbit.shared.generated.resources.member_load_failed
import orbit.shared.generated.resources.member_loading
import orbit.shared.generated.resources.member_not_found
import orbit.shared.generated.resources.member_not_found_body
import orbit.shared.generated.resources.member_role_title
import orbit.shared.generated.resources.module_schedule_time
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Where their face floats: on the top row, between the space's name and the right edge.
private const val FACE_X = 0.74f
private const val FACE_Y = 0.32f

/**
 * One member of a space, painted in their colour: their face and name, their role in the header, what they have and
 * what they did lately. Whoever may change their role taps it, which opens the role picker and, for the owner, the way to
 * hand the space over. Tapping an item opens it through [onOpenItem].
 */
@Composable
fun MemberScreen(
    spaceId: String,
    userId: String,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: MemberViewModel = koinViewModel { parametersOf(spaceId, userId) },
    membersViewModel: SpaceMembersViewModel = koinViewModel(key = "members-$spaceId") { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val members by membersViewModel.uiState.collectAsStateWithLifecycle()
    val member = members.members.firstOrNull { it.id == userId }
    OrbitSpaceColor(state.index) {
        OrbitScreen(
            topBar = {
                if (state.loaded) {
                    MemberHeader(state, member, onBack) { membersViewModel.openRolePicker(userId) }
                } else {
                    OrbitTopBar(
                        title = state.spaceName,
                        navigationIcon = {
                            OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.back), onBack)
                        },
                    )
                }
            },
            list = { memberBody(state, viewModel, onOpenItem) },
        )
        val picking = members.rolePicker?.takeIf { it.id == userId }
        if (picking != null) {
            OrbitSheet(
                title = stringResource(Res.string.member_role_title, picking.name),
                closeDescription = stringResource(Res.string.close),
                onDismiss = membersViewModel::closeRolePicker,
            ) {
                RolePickerPage(
                    member = picking,
                    busy = members.busy,
                    failed = members.failed,
                    onPick = membersViewModel::changeRole,
                    onTransfer = membersViewModel::askTransfer,
                )
            }
            if (members.confirmingTransfer) {
                TransferDialog(picking, state.spaceName, members, membersViewModel)
            }
        }
    }
}

/** The role is an action for whoever may change it, and a plain label for everyone else. */
@Composable
private fun MemberHeader(state: MemberUiState, member: SpaceMember?, onBack: () -> Unit, onOpenRole: () -> Unit) {
    val role = state.role?.let { stringResource(it) }
    val canChange = member != null && (member.canChangeRole || member.canTakeOwnership)
    OrbitColorHeader(
        title = state.name,
        label = state.spaceName,
        backDescription = stringResource(Res.string.back),
        onBack = onBack,
        detail = role?.takeUnless { canChange }?.uppercase(),
        actions = if (canChange && role != null) {
            { OrbitHeaderAction(text = role, icon = Res.drawable.ic_expand_more, onClick = onOpenRole) }
        } else {
            null
        },
        floating = {
            OrbitFloatingFaces(
                faces = listOf(
                    OrbitFloatingFace(
                        id = "member",
                        initials = state.initials,
                        color = OrbitTheme.colors.spaceAccent(state.index),
                        shape = memberShape(state.index),
                        description = state.name,
                        photoUrl = state.photoUrl,
                    ),
                ),
                modifier = Modifier.matchParentSize(),
                centerX = FACE_X,
                centerY = FACE_Y,
            )
        },
    )
}

private fun LazyListScope.memberBody(state: MemberUiState, viewModel: MemberViewModel, onOpenItem: (String) -> Unit) {
    when {
        state.loading -> item {
            OrbitLoadingState(stringResource(Res.string.member_loading), Modifier.fillMaxWidth())
        }

        state.loadFailed -> item {
            OrbitErrorState(
                title = stringResource(Res.string.member_load_failed),
                body = stringResource(Res.string.load_failed_body),
                retryText = stringResource(Res.string.retry),
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.notFound -> item {
            OrbitEmptyState(
                title = stringResource(Res.string.member_not_found),
                body = stringResource(Res.string.member_not_found_body),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.hasNothing -> item {
            OrbitEmptyState(
                title = stringResource(Res.string.member_empty_title),
                body = stringResource(Res.string.member_empty_body),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        else -> {
            if (state.has.isNotEmpty()) {
                item(key = "has") {
                    OrbitSectionHeader(stringResource(Res.string.member_has), Modifier.fillMaxWidth(), state.has.size.toString())
                }
                orbitGroup(state.has, key = { "has-${it.id}" }) { item ->
                    MemberRow(item.title, item.due?.let { itemDueLabel(it, state.today) }, done = false) { onOpenItem(item.id) }
                }
            }
            if (state.lately.isNotEmpty()) {
                item(key = "lately") { OrbitSectionHeader(stringResource(Res.string.member_lately), Modifier.fillMaxWidth()) }
                orbitGroup(state.lately, key = { "done-${it.id}" }) { done ->
                    val moment = stringResource(Res.string.module_schedule_time, itemDateLabel(done.date), done.time.toString())
                    MemberRow(done.title, moment, done = true) { onOpenItem(done.itemId) }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(title: String, moment: String?, done: Boolean, onClick: () -> Unit) {
    OrbitGroupRow(Modifier.fillMaxWidth(), onClick = onClick, done = done) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        moment?.let {
            Text(it, style = OrbitTheme.data.small, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
