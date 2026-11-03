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
import orbit.shared.generated.resources.item_history_former_member
import orbit.shared.generated.resources.member_you
import orbit.shared.generated.resources.module_schedule_time
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Who completed the item and when, newest first. Nothing at all until it has been completed once. */
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
        OrbitGroup(state.completions, key = { it.id }) { completion ->
            OrbitGroupRow(Modifier.fillMaxWidth()) {
                OrbitAvatar(
                    initials = completion.initials,
                    accent = OrbitTheme.colors.spaceAccent(completion.index),
                    photoUrl = completion.photoUrl,
                    shape = memberShape(completion.index),
                )
                Text(
                    text = when {
                        completion.isYou -> stringResource(Res.string.member_you)
                        else -> completion.name ?: stringResource(Res.string.item_history_former_member)
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        Res.string.module_schedule_time,
                        itemDateLabel(completion.date),
                        completion.time.toString(),
                    ),
                    style = OrbitTheme.data.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
