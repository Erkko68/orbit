package com.orbit.app.domain.model

/** How the user proves an account is theirs before the other way in is linked to it. */
enum class LinkProof {
    /** The account already has a password: typing it links Google to it. */
    PASSWORD,

    /** The account was created with Google: signing in with it links a password to it. */
    GOOGLE,
}

/**
 * Thrown when a sign-in stopped because [email] already belongs to one account reached another way.
 * [proof] says what the user does to prove the account is theirs; linking it then keeps one `uid`,
 * one profile and one set of spaces whichever way they sign in (#173).
 */
class AccountLinkException(
    val email: String,
    val proof: LinkProof,
    cause: Throwable? = null,
) : Exception("$email already signs in another way ($proof)", cause)
