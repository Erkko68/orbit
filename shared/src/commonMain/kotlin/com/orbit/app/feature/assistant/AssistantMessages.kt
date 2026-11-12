package com.orbit.app.feature.assistant

import com.orbit.app.domain.model.AssistantError
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.interview_error_disabled
import orbit.shared.generated.resources.interview_error_disabled_title
import orbit.shared.generated.resources.interview_error_network
import orbit.shared.generated.resources.interview_error_network_title
import orbit.shared.generated.resources.interview_error_quota
import orbit.shared.generated.resources.interview_error_quota_title
import orbit.shared.generated.resources.interview_error_unavailable
import orbit.shared.generated.resources.interview_error_unavailable_title
import orbit.shared.generated.resources.interview_error_unknown
import orbit.shared.generated.resources.interview_error_unknown_title
import org.jetbrains.compose.resources.StringResource

/** What the interview's chat says when the assistant did not answer: a title, and what the user can do. */
internal fun AssistantError.title(): StringResource = when (this) {
    AssistantError.NETWORK -> Res.string.interview_error_network_title
    AssistantError.QUOTA_EXCEEDED -> Res.string.interview_error_quota_title
    AssistantError.DISABLED -> Res.string.interview_error_disabled_title
    AssistantError.UNAVAILABLE -> Res.string.interview_error_unavailable_title
    AssistantError.UNKNOWN -> Res.string.interview_error_unknown_title
}

internal fun AssistantError.message(): StringResource = when (this) {
    AssistantError.NETWORK -> Res.string.interview_error_network
    AssistantError.QUOTA_EXCEEDED -> Res.string.interview_error_quota
    AssistantError.DISABLED -> Res.string.interview_error_disabled
    AssistantError.UNAVAILABLE -> Res.string.interview_error_unavailable
    AssistantError.UNKNOWN -> Res.string.interview_error_unknown
}
