package com.orbit.app.feature.space.ui

import com.orbit.app.domain.model.JoinError
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.join_error_already_member
import orbit.shared.generated.resources.join_error_expired
import orbit.shared.generated.resources.join_error_failed
import orbit.shared.generated.resources.join_error_invalid
import orbit.shared.generated.resources.join_error_revoked
import org.jetbrains.compose.resources.StringResource

/** What the dialog says when joining did not work. */
internal fun JoinError.message(): StringResource = when (this) {
    JoinError.INVALID -> Res.string.join_error_invalid
    JoinError.EXPIRED -> Res.string.join_error_expired
    JoinError.REVOKED -> Res.string.join_error_revoked
    JoinError.ALREADY_MEMBER -> Res.string.join_error_already_member
    JoinError.FAILED -> Res.string.join_error_failed
}
