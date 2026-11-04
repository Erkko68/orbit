package com.orbit.app.feature.item.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.component.memberShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.item.ui.itemDateLabel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.item_history
import orbit.shared.generated.resources.item_history_completed
import orbit.shared.generated.resources.item_history_created
import orbit.shared.generated.resources.item_history_former_member
import orbit.shared.generated.resources.item_history_reopened
import orbit.shared.generated.resources.item_history_you_completed
import orbit.shared.generated.resources.item_history_you_created
import orbit.shared.generated.resources.item_history_you_reopened
import orbit.shared.generated.resources.module_schedule_time
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** What happened to the item, newest first: who marked it done, reopened it and created it, and when. */
@Composable
internal fun ItemHistory(
    spaceId: String,
    itemId: String,
    modifier: Modifier = Modifier,
    viewModel: ItemHistoryViewModel = koinViewModel(key = "history-$itemId") { parametersOf(spaceId, itemId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.hasHistory) return
    Column(modifier) {
        OrbitSectionHeader(stringResource(Res.string.item_history), Modifier.fillMaxWidth())
        OrbitGroup(state.entries, key = { it.id }) { entry ->
            OrbitGroupRow(Modifier.fillMaxWidth()) {
                OrbitAvatar(
                    initials = entry.initials,
                    accent = OrbitTheme.colors.spaceAccent(entry.index),
                    photoUrl = entry.photoUrl,
                    shape = memberShape(entry.index),
                )
                Column(Modifier.weight(1f)) {
                    Text(historyLine(entry), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(Res.string.module_schedule_time, itemDateLabel(entry.date), entry.time.toString()),
                        style = OrbitTheme.data.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "Ana marked it done", "You reopened it": who did what, in words. */
@Composable
private fun historyLine(entry: ItemHistoryEntry): String {
    if (entry.isYou) {
        return stringResource(
            when (entry.action) {
                ItemHistoryAction.COMPLETED -> Res.string.item_history_you_completed
                ItemHistoryAction.REOPENED -> Res.string.item_history_you_reopened
                ItemHistoryAction.CREATED -> Res.string.item_history_you_created
            },
        )
    }
    val name = entry.name ?: stringResource(Res.string.item_history_former_member)
    return stringResource(
        when (entry.action) {
            ItemHistoryAction.COMPLETED -> Res.string.item_history_completed
            ItemHistoryAction.REOPENED -> Res.string.item_history_reopened
            ItemHistoryAction.CREATED -> Res.string.item_history_created
        },
        name,
    )
}
