package com.orbit.app.domain.model

const val MIN_PASSWORD_LENGTH = 8

/** What is wrong with the email and password typed into a sign-in or create-account form. */
sealed interface CredentialsError {
    data object BlankEmail : CredentialsError
    data object InvalidEmail : CredentialsError
    data object BlankPassword : CredentialsError
    data object ShortPassword : CredentialsError
    data object PasswordsDiffer : CredentialsError
}

/**
 * The checks before signing in. The password is only checked for being there: an account made
 * before the length rule may have a shorter one. Returns the errors, so a form can show them all.
 */
fun validateSignIn(email: String, password: String): List<CredentialsError> =
    emailErrors(email) + listOfNotNull(CredentialsError.BlankPassword.takeIf { password.isEmpty() })

/** The checks before creating an account: a valid email, a long enough password, typed twice the same. */
fun validateRegistration(email: String, password: String, confirmation: String): List<CredentialsError> =
    emailErrors(email) + newPasswordErrors(password, confirmation)

/** The checks before changing the password: the current one typed, then the new one as for creating an account. */
fun validatePasswordChange(current: String, new: String, confirmation: String): List<CredentialsError> =
    listOfNotNull(CredentialsError.BlankPassword.takeIf { current.isEmpty() }) + newPasswordErrors(new, confirmation)

private fun newPasswordErrors(password: String, confirmation: String): List<CredentialsError> = listOfNotNull(
    CredentialsError.ShortPassword.takeIf { password.length < MIN_PASSWORD_LENGTH },
    CredentialsError.PasswordsDiffer.takeIf { confirmation != password },
)

private fun emailErrors(email: String): List<CredentialsError> {
    val trimmed = email.trim()
    return listOfNotNull(
        CredentialsError.BlankEmail.takeIf { trimmed.isEmpty() },
        CredentialsError.InvalidEmail.takeIf { trimmed.isNotEmpty() && !looksLikeAnEmail(trimmed) },
    )
}

/** One `@` between a name and a domain with a dot inside it, and no spaces. Firebase has the last word. */
private fun looksLikeAnEmail(email: String): Boolean {
    if (email.any { it.isWhitespace() }) return false
    val parts = email.split('@')
    if (parts.size != 2 || parts[0].isEmpty()) return false
    val domain = parts[1]
    return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
}
