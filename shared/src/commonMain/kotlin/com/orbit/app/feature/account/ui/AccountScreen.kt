package com.orbit.app.feature.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitListCard
import com.orbit.app.core.designsystem.component.OrbitListItem
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.account.AccountDialog
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_back
import orbit.shared.generated.resources.account_change_password
import orbit.shared.generated.resources.account_change_password_body
import orbit.shared.generated.resources.account_delete
import orbit.shared.generated.resources.account_delete_body
import orbit.shared.generated.resources.account_edit_name
import orbit.shared.generated.resources.account_sign_out
import orbit.shared.generated.resources.account_sign_out_body
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_delete
import orbit.shared.generated.resources.ic_edit
import orbit.shared.generated.resources.ic_lock
import orbit.shared.generated.resources.ic_logout
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The signed-in user's account: who they are on an elevated card, then the account's actions, each a
 * card that opens its dialog. Signing out or deleting the account moves the auth gate to sign-in.
 */
@Composable
fun AccountScreen(onBack: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.account_title),
                navigationIcon = {
                    OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.account_back), onBack)
                },
            )
        },
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg),
        scrollable = true,
    ) {
        OrbitCard(Modifier.fillMaxWidth(), elevated = true) {
            OrbitListItem(
                title = state.name,
                supporting = state.email,
                leading = { OrbitAvatar(state.initials, MaterialTheme.colorScheme.primary, photoUrl = state.photoUrl) },
                trailing = {
                    OrbitIconButton(
                        icon = Res.drawable.ic_edit,
                        contentDescription = stringResource(Res.string.account_edit_name),
                        onClick = viewModel::openEditName,
                    )
                },
            )
        }
        OrbitListCard(
            title = stringResource(Res.string.account_change_password),
            supporting = stringResource(Res.string.account_change_password_body),
            icon = Res.drawable.ic_lock,
            modifier = Modifier.fillMaxWidth(),
            onClick = viewModel::openChangePassword,
        )
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            OrbitListCard(
                title = stringResource(Res.string.account_sign_out),
                supporting = stringResource(Res.string.account_sign_out_body),
                icon = Res.drawable.ic_logout,
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::openSignOut,
            )
            OrbitListCard(
                title = stringResource(Res.string.account_delete),
                supporting = stringResource(Res.string.account_delete_body),
                icon = Res.drawable.ic_delete,
                modifier = Modifier.fillMaxWidth(),
                onClick = viewModel::openDeleteAccount,
                iconTint = MaterialTheme.colorScheme.error,
            )
        }
    }
    when (state.dialog) {
        AccountDialog.EDIT_NAME -> EditNameDialog(state, viewModel)
        AccountDialog.CHANGE_PASSWORD -> ChangePasswordDialog(state, viewModel)
        AccountDialog.SIGN_OUT -> SignOutDialog(state, viewModel)
        AccountDialog.DELETE_ACCOUNT -> DeleteAccountDialog(state, viewModel)
        null -> Unit
    }
}
