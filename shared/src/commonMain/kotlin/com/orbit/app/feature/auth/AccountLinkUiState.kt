package com.orbit.app.feature.auth

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.LinkProof
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.account_new_password
import orbit.shared.generated.resources.link_google_body
import orbit.shared.generated.resources.link_google_confirm
import orbit.shared.generated.resources.link_google_title
import orbit.shared.generated.resources.link_password_body
import orbit.shared.generated.resources.link_password_confirm
import orbit.shared.generated.resources.link_password_title
import orbit.shared.generated.resources.sign_in_password
import org.jetbrains.compose.resources.StringResource

/**
 * The one account an address already belongs to, while the user proves it is theirs so the other way in
 * is linked to it instead of a second account being made (#173). A collision is this state, never a
 * thrown error the form shows as "something went wrong".
 */
data class AccountLinkUiState(
    val email: String,
    val proof: LinkProof,
    val password: String = "",
    val passwordVisible: Boolean = false,
    /** A link in progress, which disables the button so it cannot run twice. */
    val linking: Boolean = false,
    /** Why the last link attempt failed. Cleared by the next edit or attempt. */
    val failed: AuthError? = null,
) {
    val title: StringResource
        get() = when (proof) {
            LinkProof.PASSWORD -> Res.string.link_password_title
            LinkProof.GOOGLE -> Res.string.link_google_title
        }

    /** What the sheet explains, with the address as its one placeholder. */
    val body: StringResource
        get() = when (proof) {
            LinkProof.PASSWORD -> Res.string.link_password_body
            LinkProof.GOOGLE -> Res.string.link_google_body
        }

    val confirm: StringResource
        get() = when (proof) {
            LinkProof.PASSWORD -> Res.string.link_password_confirm
            LinkProof.GOOGLE -> Res.string.link_google_confirm
        }

    /** The password asked for: the one the account has, or the one it is about to get. */
    val passwordPlaceholder: StringResource
        get() = when (proof) {
            LinkProof.PASSWORD -> Res.string.sign_in_password
            LinkProof.GOOGLE -> Res.string.account_new_password
        }

    /** Whether the user may continue with Google instead of typing a password. */
    val googleOffered: Boolean get() = proof == LinkProof.GOOGLE

    val canLink: Boolean get() = password.isNotBlank() && !linking

    val passwordError: StringResource? get() = failed?.message()
}
