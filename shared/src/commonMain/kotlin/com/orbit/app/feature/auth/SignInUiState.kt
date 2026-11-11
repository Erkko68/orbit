package com.orbit.app.feature.auth

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.CredentialsError
import org.jetbrains.compose.resources.StringResource

data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    override val submitting: Boolean = false,
    /** What the form got wrong, found when Sign in was pressed. Cleared by the next edit. */
    val errors: List<CredentialsError> = emptyList(),
    /** Why the last attempt failed. Cleared by the next edit or attempt. */
    val authError: AuthError? = null,
    /** The account this address already belongs to, while the user links the other way in (#173). */
    override val link: AccountLinkUiState? = null,
    /** The reset-password dialog is open (#217). */
    val resetOpen: Boolean = false,
    /** The address the dialog will send to. It starts as whatever the form already has. */
    val resetEmail: String = "",
    /** What is wrong with that address, found when Send was pressed. Cleared by the next edit. */
    val resetErrors: List<CredentialsError> = emptyList(),
    /** The link is on its way out. */
    val resetSending: Boolean = false,
    /** The dialog has answered. It says the same whether or not the address has an account. */
    val resetSent: Boolean = false,
    /** Why sending failed. Cleared by the next edit or attempt. */
    val resetError: AuthError? = null,
) : AuthFormState<SignInUiState> {
    override fun starting() = copy(submitting = true, errors = emptyList(), authError = null)

    override fun finished(link: AccountLinkUiState?, authError: AuthError?) =
        copy(submitting = false, link = link ?: this.link, authError = authError)

    override fun withLink(link: AccountLinkUiState?) = copy(link = link)

    /** What to say under the email field. */
    val emailError: StringResource? get() = emailMessage(errors, authError)

    /** Send is offered once there is something to send and nothing already going. */
    val canSendReset: Boolean get() = !resetSending && resetEmail.isNotBlank()

    /** What to say under the reset dialog's email field. */
    val resetEmailError: StringResource? get() = emailMessage(resetErrors, resetError)

    /** Why sending the link failed, which is the only failure the dialog ever shows. */
    val resetFailure: StringResource? get() = otherAuthMessage(resetError)

    /** What to say under the password, the last field: a blank password, or why Firebase refused. */
    val passwordError: StringResource?
        get() = errors.firstOrNull { it == CredentialsError.BlankPassword }?.message() ?: otherAuthMessage(authError)
}
