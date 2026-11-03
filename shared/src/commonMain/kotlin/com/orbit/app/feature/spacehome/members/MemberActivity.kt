package com.orbit.app.feature.spacehome.members

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.app.core.designsystem.component.OrbitEmptyState
import com.orbit.app.core.designsystem.component.OrbitGroup
import com.orbit.app.core.designsystem.component.OrbitGroupRow
import com.orbit.app.core.designsystem.component.OrbitSectionHeader
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.item.ui.itemDateLabel
import com.orbit.app.feature.item.ui.itemDueLabel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.member_empty_body
import orbit.shared.generated.resources.member_empty_title
import orbit.shared.generated.resources.member_has
import orbit.shared.generated.resources.member_lately
import orbit.shared.generated.resources.module_schedule_time
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Under a member's [MemberHeader] in the settings sheet: what they have, then what they did lately. It loads by
 * itself, so the header shows at once and this fills in under it. An item opens through [onOpenItem].
 */
@Composable
internal fun MemberActivity(
    spaceId: String,
    userId: String,
    onOpenItem: (String) -> Unit,
    viewModel: MemberActivityViewModel = koinViewModel(key = "member-$spaceId-$userId") { parametersOf(spaceId, userId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.hasNothing) {
        OrbitEmptyState(
            title = stringResource(Res.string.member_empty_title),
            body = stringResource(Res.string.member_empty_body),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (state.has.isNotEmpty()) {
        OrbitSectionHeader(stringResource(Res.string.member_has), Modifier.fillMaxWidth(), state.has.size.toString())
        OrbitGroup(state.has, key = { it.id }, modifier = Modifier.fillMaxWidth()) { item ->
            ActivityRow(item.title, item.due?.let { itemDueLabel(it, state.today) }, done = false) { onOpenItem(item.id) }
        }
    }
    if (state.lately.isNotEmpty()) {
        OrbitSectionHeader(stringResource(Res.string.member_lately), Modifier.fillMaxWidth())
        OrbitGroup(state.lately, key = { it.id }, modifier = Modifier.fillMaxWidth()) { done ->
            val moment = stringResource(Res.string.module_schedule_time, itemDateLabel(done.date), done.time.toString())
            ActivityRow(done.title, moment, done = true) { onOpenItem(done.itemId) }
        }
    }
}

@Composable
private fun ActivityRow(title: String, moment: String?, done: Boolean, onClick: () -> Unit) {
    OrbitGroupRow(Modifier.fillMaxWidth(), onClick = onClick, done = done) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        moment?.let {
            Text(it, style = OrbitTheme.data.small, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
