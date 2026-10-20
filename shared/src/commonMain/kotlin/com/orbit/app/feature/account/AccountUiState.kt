package com.orbit.app.feature.account

import com.orbit.app.core.initialsOf
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.CredentialsError
import com.orbit.app.feature.auth.message
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.error_current_password_wrong
import org.jetbrains.compose.resources.StringResource

/** The task over the account screen, one at a time. */
enum class AccountDialog { EDIT_NAME, CHANGE_PASSWORD, SIGN_OUT, DELETE_ACCOUNT }

data class AccountUiState(
    /** The signed-in user, from the session. */
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    /** The open dialog, if any. */
    val dialog: AccountDialog? = null,
    /** What is typed in the edit-name dialog. */
    val nameDraft: String = "",
    /** What is typed in the change-password dialog. */
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    /** Shows the three passwords as text. */
    val passwordsVisible: Boolean = false,
    /** What the change-password form got wrong. Cleared by the next edit. */
    val passwordErrors: List<CredentialsError> = emptyList(),
    /** A dialog's write is running: its confirm button waits for it. */
    val submitting: Boolean = false,
    /** Why the open dialog's last write failed. Cleared by the next edit or attempt. */
    val authError: AuthError? = null,
) {
    val initials: String get() = initialsOf(name)

    val canSaveName: Boolean get() = nameDraft.isNotBlank() && nameDraft.trim() != name && !submitting

    /** Under the edit-name field, or in the sign-out and delete dialogs. */
    val dialogError: StringResource? get() = authError?.message()

    val currentPasswordError: StringResource?
        get() = passwordErrors.firstOrNull { it == CredentialsError.BlankPassword }?.message()
            ?: Res.string.error_current_password_wrong.takeIf { authError == AuthError.WRONG_CREDENTIALS }

    val newPasswordError: StringResource?
        get() = passwordErrors.firstOrNull { it == CredentialsError.ShortPassword }?.message()
            ?: authError?.takeIf { it == AuthError.WEAK_PASSWORD }?.message()

    /** Under the last field: the passwords differ, or why the change failed if not about one of them. */
    val confirmationError: StringResource?
        get() = passwordErrors.firstOrNull { it == CredentialsError.PasswordsDiffer }?.message()
            ?: authError?.takeIf { it != AuthError.WRONG_CREDENTIALS && it != AuthError.WEAK_PASSWORD }?.message()
}
