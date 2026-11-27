package com.orbit.app.feature.activity.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.dateLabel
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitErrorState
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitIconButton
import com.orbit.app.core.designsystem.component.OrbitLoadingState
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.OrbitTopBar
import com.orbit.app.core.designsystem.component.orbitGroup
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.ActivityAction
import com.orbit.app.feature.activity.ActivityDay
import com.orbit.app.feature.activity.ActivityEntry
import com.orbit.app.feature.activity.ActivityViewModel
import com.orbit.app.feature.activity.RelativeDay
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.activity_by_assistant
import orbit.shared.generated.resources.activity_complete
import orbit.shared.generated.resources.activity_complete_you
import orbit.shared.generated.resources.activity_create
import orbit.shared.generated.resources.activity_create_you
import orbit.shared.generated.resources.activity_delete
import orbit.shared.generated.resources.activity_delete_you
import orbit.shared.generated.resources.activity_edit
import orbit.shared.generated.resources.activity_edit_you
import orbit.shared.generated.resources.activity_empty_body
import orbit.shared.generated.resources.activity_empty_title
import orbit.shared.generated.resources.activity_load_failed
import orbit.shared.generated.resources.activity_loading
import orbit.shared.generated.resources.activity_reopen
import orbit.shared.generated.resources.activity_reopen_you
import orbit.shared.generated.resources.activity_someone
import orbit.shared.generated.resources.activity_title
import orbit.shared.generated.resources.back
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.item_due_today
import orbit.shared.generated.resources.item_due_yesterday
import orbit.shared.generated.resources.load_failed_body
import orbit.shared.generated.resources.retry
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * What changed in a space in the last 30 days (#83), a route from its settings: one group of rows per day, newest
 * first. A change a confirmed assistant plan made says so under it.
 */
@Composable
fun ActivityScreen(
    spaceId: String,
    onBack: () -> Unit,
    viewModel: ActivityViewModel = koinViewModel { parametersOf(spaceId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OrbitScreen(
        topBar = {
            OrbitTopBar(
                title = stringResource(Res.string.activity_title),
                navigationIcon = { OrbitIconButton(Res.drawable.ic_arrow_back, stringResource(Res.string.back), onBack) },
            )
        },
        list = {
            when {
                state.loading -> item {
                    OrbitLoadingState(stringResource(Res.string.activity_loading), Modifier.fillMaxWidth())
                }

                state.loadFailed -> item {
                    OrbitErrorState(
                        title = stringResource(Res.string.activity_load_failed),
                        body = stringResource(Res.string.load_failed_body),
                        retryText = stringResource(Res.string.retry),
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                state.isEmpty -> item {
                    OrbitEmptyState(
                        title = stringResource(Res.string.activity_empty_title),
                        body = stringResource(Res.string.activity_empty_body),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> state.days.forEach { day ->
                    item(key = day.date.toString()) {
                        OrbitSectionHeader(dayLabel(day), Modifier.fillMaxWidth())
                    }
                    orbitGroup(day.entries, key = { it.id }) { EntryRow(it) }
                }
            }
        },
    )
}

@Composable
private fun dayLabel(day: ActivityDay): String = when (day.relative) {
    RelativeDay.TODAY -> stringResource(Res.string.item_due_today)
    RelativeDay.YESTERDAY -> stringResource(Res.string.item_due_yesterday)
    RelativeDay.EARLIER -> dateLabel(day.date)
}

@Composable
private fun EntryRow(entry: ActivityEntry) {
    OrbitGroupRow(Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(
                text = sentence(entry),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val time = entry.time.toString()
            Text(
                text = if (entry.byAssistant) stringResource(Res.string.activity_by_assistant, time) else time,
                style = OrbitTheme.data.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/** "Ana completed Dinner", or "You completed Dinner" for the user. */
@Composable
private fun sentence(entry: ActivityEntry): String {
    val (theirs, yours) = phrasing(entry.action)
    return if (entry.byYou) {
        stringResource(yours, entry.title)
    } else {
        stringResource(theirs, entry.actor ?: stringResource(Res.string.activity_someone), entry.title)
    }
}

private fun phrasing(action: ActivityAction): Pair<StringResource, StringResource> = when (action) {
    ActivityAction.CREATE -> Res.string.activity_create to Res.string.activity_create_you
    ActivityAction.EDIT -> Res.string.activity_edit to Res.string.activity_edit_you
    ActivityAction.COMPLETE -> Res.string.activity_complete to Res.string.activity_complete_you
    ActivityAction.REOPEN -> Res.string.activity_reopen to Res.string.activity_reopen_you
    ActivityAction.DELETE -> Res.string.activity_delete to Res.string.activity_delete_you
}
