package com.orbit.app.domain.model

/** Why joining a space with an invitation did not work, for the dialog to say so. */
enum class JoinError {
    /** The code is not an invitation code, or no invitation has it. */
    INVALID,

    /** The invitation is past its expiry. */
    EXPIRED,

    /** An owner or admin revoked the invitation. */
    REVOKED,

    /** The user is in the space already. */
    ALREADY_MEMBER,

    /** Anything else, such as no connection. */
    FAILED,
}

/** A join the invitation itself refuses: [error] is never [JoinError.FAILED]. */
class JoinException(val error: JoinError) : Exception(error.name)
