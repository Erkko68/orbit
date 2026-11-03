package com.orbit.app.feature.assistant

import com.orbit.app.domain.model.AssistantError
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.interview_error_disabled
import orbit.shared.generated.resources.interview_error_network
import orbit.shared.generated.resources.interview_error_quota
import orbit.shared.generated.resources.interview_error_unavailable
import orbit.shared.generated.resources.interview_error_unknown
import org.jetbrains.compose.resources.StringResource

/** What the interview says when the assistant did not answer. */
internal fun AssistantError.message(): StringResource = when (this) {
    AssistantError.NETWORK -> Res.string.interview_error_network
    AssistantError.QUOTA_EXCEEDED -> Res.string.interview_error_quota
    AssistantError.DISABLED -> Res.string.interview_error_disabled
    AssistantError.UNAVAILABLE -> Res.string.interview_error_unavailable
    AssistantError.UNKNOWN -> Res.string.interview_error_unknown
}
