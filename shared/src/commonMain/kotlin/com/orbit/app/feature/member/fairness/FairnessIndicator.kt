package com.orbit.app.feature.member.fairness

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitHeading
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.domain.usecase.EffortShare
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.fairness_empty
import orbit.shared.generated.resources.fairness_failed
import orbit.shared.generated.resources.fairness_period
import orbit.shared.generated.resources.fairness_share
import orbit.shared.generated.resources.fairness_points
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.stringResource

/** An honest loading/error/empty state: a failed read never looks like zero effort. */
@Composable
internal fun FairnessStatus(state: FairnessUiState, onRetry: () -> Unit) {
    when {
        state.loadFailed -> OrbitErrorState(
            stringResource(Res.string.fairness_failed), stringResource(Res.string.load_failed_body),
            stringResource(Res.string.retry), onRetry, Modifier.fillMaxWidth(),
        )

        state.loading -> OrbitLoadingState(stringResource(Res.string.fairness_period), Modifier.fillMaxWidth())
        !state.hasPoints -> Text(stringResource(Res.string.fairness_empty), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Shared wording for the exact same comparison in settings and on a member's page. */
@Composable
internal fun fairnessLabel(share: EffortShare): String = stringResource(
    Res.string.fairness_share,
    share.roundedPercentage,
    share.roundedEqualPercentage,
)

@Composable
internal fun MemberEffort(share: EffortShare) {
    OrbitHeading(
        title = fairnessLabel(share),
        subtitle = stringResource(Res.string.fairness_points, share.points, share.totalPoints),
        modifier = Modifier.fillMaxWidth(),
    )
}
