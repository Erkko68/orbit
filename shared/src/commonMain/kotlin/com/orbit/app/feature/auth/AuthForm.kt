package com.orbit.app.feature.auth

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The part of an auth form that submitting moves. Sign-in and create account hold different fields but
 * the same answers, so each describes them here and both share one [AuthForm] (#173).
 */
interface AuthFormState<S : AuthFormState<S>> {
    val submitting: Boolean

    /** The account this address already belongs to, while the user links the other way in. */
    val link: AccountLinkUiState?

    /** Busy, with nothing wrong yet. */
    fun starting(): S

    /** Done, with what it left behind: an account to link, a reason it failed, or neither. */
    fun finished(link: AccountLinkUiState? = null, authError: AuthError? = null): S

    /** The link sheet opening, changing or closing. */
    fun withLink(link: AccountLinkUiState?): S
}

/**
 * Runs an auth form's submissions and owns its link sheet, so neither form writes the same update
 * twice (#173).
 */
internal class AuthForm<S : AuthFormState<S>>(
    auth: AuthRepository,
    private val state: MutableStateFlow<S>,
    private val scope: CoroutineScope,
) {
    val linking = AccountLinking(
        auth = auth,
        scope = scope,
        read = { state.value.link },
        write = { link -> state.update { it.withLink(link) } },
    )

    /**
     * Runs [call] unless one is running. A failure stays in the form; an address that already has an
     * account becomes the offer to link it, not a failure the user is blamed for.
     */
    fun submit(what: String, call: suspend () -> Unit) {
        if (state.value.submitting) return
        state.update { it.starting() }
        scope.launch {
            val outcome = executeSignIn(what, call)
            state.update { form ->
                when (outcome) {
                    null -> form.finished()
                    is SignInOutcome.Link -> form.finished(link = AccountLinkUiState(outcome.email, outcome.proof))
                    is SignInOutcome.Failed -> form.finished(authError = outcome.error)
                }
            }
        }
    }
}
