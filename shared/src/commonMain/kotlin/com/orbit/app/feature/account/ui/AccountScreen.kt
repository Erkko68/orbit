package com.orbit.app.feature.account.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitAvatarSize
import com.orbit.app.core.designsystem.component.OrbitColorHeader
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.account.AccountSheetKind
import com.orbit.app.feature.account.AccountViewModel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_back
import orbit.shared.generated.resources.account_change_password
import orbit.shared.generated.resources.account_change_password_body
import orbit.shared.generated.resources.account_connect_google_body
import orbit.shared.generated.resources.account_connect_google
import orbit.shared.generated.resources.account_set_password_body
import orbit.shared.generated.resources.account_set_password
import orbit.shared.generated.resources.account_delete
import orbit.shared.generated.resources.account_delete_body
import orbit.shared.generated.resources.account_edit_name
import orbit.shared.generated.resources.account_edit_name_body
import orbit.shared.generated.resources.account_sign_out
import orbit.shared.generated.resources.account_sign_out_body
import orbit.shared.generated.resources.account_title
import orbit.shared.generated.resources.ic_delete
import orbit.shared.generated.resources.ic_edit
import orbit.shared.generated.resources.ic_link
import orbit.shared.generated.resources.ic_lock
import orbit.shared.generated.resources.ic_logout
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** One thing the user can do with their account. */
private class AccountAction(
    val title: String,
    val supporting: String,
    val icon: DrawableResource,
    val shape: OrbitShape,
    val onClick: () -> Unit,
    val danger: Boolean = false,
)

/**
 * The signed-in user's account: a colour block with who they are, then what they can do, as connected groups. Each row
 * opens its dialog. Signing out or deleting the account moves the auth gate to sign-in.
 */
@Composable
fun AccountScreen(onBack: () -> Unit, viewModel: AccountViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        topBar = {
            OrbitColorHeader(
                title = state.name,
                label = stringResource(Res.string.account_title),
                backDescription = stringResource(Res.string.account_back),
                onBack = onBack,
                detail = state.email,
                leading = {
                    OrbitAvatar(
                        initials = state.initials,
                        accent = Ink,
                        contentColor = OrbitTheme.accent.vivid,
                        photoUrl = state.photoUrl,
                        shape = OrbitShape.Cookie,
                        size = OrbitAvatarSize.Hero,
                    )
                },
            )
        },
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg),
        scrollable = true,
    ) {
        // An account that only signs in with Google sets a password instead of changing one, and one without
        // Google can connect it: both ways in reach the same account (#293).
        AccountGroup(
            *listOfNotNull(
                AccountAction(
                    title = stringResource(Res.string.account_edit_name),
                    supporting = stringResource(Res.string.account_edit_name_body),
                    icon = Res.drawable.ic_edit,
                    shape = OrbitShape.Cookie,
                    onClick = viewModel::openEditName,
                ),
                if (state.hasPassword) {
                    AccountAction(
                        title = stringResource(Res.string.account_change_password),
                        supporting = stringResource(Res.string.account_change_password_body),
                        icon = Res.drawable.ic_lock,
                        shape = OrbitShape.Clover,
                        onClick = viewModel::openChangePassword,
                    )
                } else {
                    AccountAction(
                        title = stringResource(Res.string.account_set_password),
                        supporting = stringResource(Res.string.account_set_password_body),
                        icon = Res.drawable.ic_lock,
                        shape = OrbitShape.Clover,
                        onClick = viewModel::openChangePassword,
                    )
                },
                AccountAction(
                    title = stringResource(Res.string.account_connect_google),
                    supporting = stringResource(Res.string.account_connect_google_body),
                    icon = Res.drawable.ic_link,
                    shape = OrbitShape.Flower,
                    onClick = viewModel::openConnectGoogle,
                ).takeIf { !state.signsInWithGoogle },
            ).toTypedArray(),
        )
        AccountGroup(
            AccountAction(
                title = stringResource(Res.string.account_sign_out),
                supporting = stringResource(Res.string.account_sign_out_body),
                icon = Res.drawable.ic_logout,
                shape = OrbitShape.Sunny,
                onClick = viewModel::openSignOut,
            ),
            AccountAction(
                title = stringResource(Res.string.account_delete),
                supporting = stringResource(Res.string.account_delete_body),
                icon = Res.drawable.ic_delete,
                shape = OrbitShape.Burst,
                onClick = viewModel::openDeleteAccount,
                danger = true,
            ),
        )
    }
    when (state.dialog) {
        AccountSheetKind.EDIT_NAME -> EditNameSheet(state, viewModel)
        AccountSheetKind.CHANGE_PASSWORD -> ChangePasswordSheet(state, viewModel)
        AccountSheetKind.CONNECT_GOOGLE -> ConnectGoogleSheet(state, viewModel)
        AccountSheetKind.SIGN_OUT -> SignOutSheet(state, viewModel)
        AccountSheetKind.DELETE_ACCOUNT -> DeleteAccountSheet(state, viewModel)
        null -> Unit
    }
}

@Composable
private fun AccountGroup(vararg actions: AccountAction) {
    OrbitGroup(actions.toList(), key = { it.title }, modifier = Modifier.fillMaxWidth()) { action ->
        OrbitActionRow(
            title = action.title,
            supporting = action.supporting,
            icon = action.icon,
            shape = action.shape,
            danger = action.danger,
            onClick = action.onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
