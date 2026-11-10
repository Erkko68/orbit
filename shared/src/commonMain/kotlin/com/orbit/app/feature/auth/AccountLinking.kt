package com.orbit.app.feature.auth

import com.orbit.app.domain.model.LinkProof
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The linking part of an auth form: it keeps the [AccountLinkUiState] where the form itself holds it,
 * through [read] and [write], so sign-in and create account share one implementation (#173). A linked
 * account is signed in, which moves the auth gate, so success only closes the sheet.
 */
internal class AccountLinking(
    private val auth: AuthRepository,
    private val scope: CoroutineScope,
    private val read: () -> AccountLinkUiState?,
    private val write: (AccountLinkUiState?) -> Unit,
) {
    fun onPasswordChange(password: String) = update { it.copy(password = password, failed = null) }

    fun togglePasswordVisibility() = update { it.copy(passwordVisible = !it.passwordVisible) }

    /** Leaves the address unlinked: the form is still there, with what the user typed. */
    fun dismiss() = write(null)

    fun confirm() {
        val link = read() ?: return
        if (!link.canLink) return
        write(link.copy(linking = true, failed = null))
        scope.launch {
            val failure = executeAuth("Account link") {
                when (link.proof) {
                    LinkProof.PASSWORD -> auth.linkGoogleWithPassword(link.email, link.password)
                    LinkProof.GOOGLE -> auth.setPasswordWithGoogle(link.email, link.password)
                }
            }
            if (failure == null) write(null) else update { it.copy(linking = false, failed = failure) }
        }
    }

    private fun update(change: (AccountLinkUiState) -> AccountLinkUiState) {
        read()?.let { write(change(it)) }
    }
}
