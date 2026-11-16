package com.orbit.app.feature.member.away

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitActionRow
import com.orbit.app.core.designsystem.component.OrbitDatePicker
import com.orbit.app.core.designsystem.component.OrbitErrorText
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitSheet
import com.orbit.app.core.designsystem.component.OrbitSheetPages
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.dateLabel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.close
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.member_away
import orbit.shared.generated.resources.member_away_clear
import orbit.shared.generated.resources.member_away_failed
import orbit.shared.generated.resources.member_away_from
import orbit.shared.generated.resources.member_away_last
import orbit.shared.generated.resources.member_away_load_failed
import orbit.shared.generated.resources.member_away_none
import orbit.shared.generated.resources.member_away_save
import orbit.shared.generated.resources.member_away_until
import orbit.shared.generated.resources.schedule_pick
import orbit.shared.generated.resources.schedule_pick_date
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * On the user's own member screen: the Away row, which says when they are away and opens a sheet to set the first and
 * last day or clear them (#48). It shows nothing while the member loads.
 */
@Composable
internal fun MemberAway(
    spaceId: String,
    userId: String,
    modifier: Modifier = Modifier,
    viewModel: AwayViewModel = koinViewModel(key = "away-$spaceId-$userId") { parametersOf(spaceId, userId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) return
    OrbitActionRow(
        title = stringResource(Res.string.member_away),
        supporting = state.until?.let { stringResource(Res.string.member_away_until, dateLabel(it)) }
            ?: stringResource(if (state.loadFailed) Res.string.member_away_load_failed else Res.string.member_away_none),
        icon = Res.drawable.ic_event,
        shape = OrbitShape.Sunny,
        onClick = if (state.loadFailed) viewModel::retry else viewModel::open,
        modifier = modifier,
    )
    if (state.editing) AwaySheet(state, viewModel)
}

/** The days of the period as two rows, each opening the date picker in the same sheet, and the buttons below. */
@Composable
private fun AwaySheet(state: AwayUiState, viewModel: AwayViewModel) {
    OrbitSheet(
        title = stringResource(if (state.picking == null) Res.string.member_away else Res.string.schedule_pick_date),
        closeDescription = stringResource(Res.string.close),
        onDismiss = viewModel::dismiss,
        backDescription = stringResource(Res.string.back),
        onBack = if (state.picking == null) null else viewModel::dismissPicker,
        confirmText = if (state.picking == null) stringResource(Res.string.member_away_save) else null,
        onConfirm = viewModel::save,
        confirmEnabled = state.canSave,
    ) {
        OrbitSheetPages(state.picking) { picking ->
            if (picking == null) {
                val days = listOf(AwayEnd.FROM, AwayEnd.UNTIL)
                OrbitGroup(days, key = { it.name }, modifier = Modifier.fillMaxWidth()) { end ->
                    val day = if (end == AwayEnd.FROM) state.draftFrom else state.draftUntil
                    OrbitActionRow(
                        title = stringResource(
                            if (end == AwayEnd.FROM) Res.string.member_away_from else Res.string.member_away_last,
                        ),
                        supporting = day?.let { dateLabel(it) },
                        icon = Res.drawable.ic_event,
                        shape = if (end == AwayEnd.FROM) OrbitShape.Cookie else OrbitShape.Clover,
                        onClick = { viewModel.pick(end) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.failed) OrbitErrorText(stringResource(Res.string.member_away_failed))
                if (state.isAway) {
                    OrbitTextButton(
                        text = stringResource(Res.string.member_away_clear),
                        onClick = viewModel::clear,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.busy,
                    )
                }
            } else {
                val earliest = state.earliestDay
                OrbitDatePicker(
                    selected = state.pickedDay,
                    pickText = stringResource(Res.string.schedule_pick),
                    onPick = viewModel::pickDate,
                    canPick = { earliest == null || it >= earliest },
                )
            }
        }
    }
}
