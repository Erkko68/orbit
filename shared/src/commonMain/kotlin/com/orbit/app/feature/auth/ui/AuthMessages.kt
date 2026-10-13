package com.orbit.app.feature.auth.ui

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.CredentialsError
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.error_email_blank
import orbit.shared.generated.resources.error_email_in_use
import orbit.shared.generated.resources.error_email_invalid
import orbit.shared.generated.resources.error_network
import orbit.shared.generated.resources.error_password_blank
import orbit.shared.generated.resources.error_password_short
import orbit.shared.generated.resources.error_passwords_differ
import orbit.shared.generated.resources.error_unknown
import orbit.shared.generated.resources.error_weak_password
import orbit.shared.generated.resources.error_wrong_credentials
import org.jetbrains.compose.resources.StringResource

/** The message for what is wrong with a form field. */
internal fun CredentialsError.message(): StringResource = when (this) {
    CredentialsError.BlankEmail -> Res.string.error_email_blank
    CredentialsError.InvalidEmail -> Res.string.error_email_invalid
    CredentialsError.BlankPassword -> Res.string.error_password_blank
    CredentialsError.ShortPassword -> Res.string.error_password_short
    CredentialsError.PasswordsDiffer -> Res.string.error_passwords_differ
}

/** The message for why Firebase refused. */
internal fun AuthError.message(): StringResource = when (this) {
    AuthError.WRONG_CREDENTIALS -> Res.string.error_wrong_credentials
    AuthError.INVALID_EMAIL -> Res.string.error_email_invalid
    AuthError.EMAIL_IN_USE -> Res.string.error_email_in_use
    AuthError.WEAK_PASSWORD -> Res.string.error_weak_password
    AuthError.NETWORK -> Res.string.error_network
    AuthError.UNKNOWN -> Res.string.error_unknown
}

private val emailAuthErrors = setOf(AuthError.INVALID_EMAIL, AuthError.EMAIL_IN_USE)

/** What to say under the email field: what is wrong with it, or why Firebase refused it. Null when nothing. */
internal fun emailMessage(errors: List<CredentialsError>, authError: AuthError?): StringResource? =
    errors.firstOrNull { it == CredentialsError.BlankEmail || it == CredentialsError.InvalidEmail }?.message()
        ?: authError?.takeIf { it in emailAuthErrors }?.message()

/** The part of [authError] that is not about the email: it shows under the last field of the form. */
internal fun otherAuthMessage(authError: AuthError?): StringResource? =
    authError?.takeIf { it !in emailAuthErrors }?.message()
