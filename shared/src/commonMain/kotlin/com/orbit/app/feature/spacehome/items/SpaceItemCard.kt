package com.orbit.app.feature.spacehome.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.orbit.app.core.designsystem.component.OrbitAvatar
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitIconBadge
import com.orbit.app.core.designsystem.component.OrbitInset
import com.orbit.app.core.designsystem.component.OrbitPill
import com.orbit.app.core.designsystem.component.OrbitProgress
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.item.ui.itemDateLabel
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_chevron_right
import orbit.shared.generated.resources.ic_event
import orbit.shared.generated.resources.ic_person
import orbit.shared.generated.resources.item_complete
import orbit.shared.generated.resources.item_reopen
import orbit.shared.generated.resources.module_checklist_title
import orbit.shared.generated.resources.space_item_checklist_progress
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** An item's summary, with its modules and the existing complete or reopen action. */
@Composable
internal fun SpaceItemCard(
    item: SpaceItem,
    onClick: () -> Unit,
    onDoneChange: (Boolean) -> Unit,
    submitting: Boolean,
    modifier: Modifier = Modifier,
) {
    OrbitCard(modifier = modifier, onClick = onClick, elevated = true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OrbitIconBadge(item.icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
                OrbitPill(stringResource(item.label))
                Text(
                    text = item.title,
                    textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            val completeDescription =
                stringResource(if (item.done) Res.string.item_reopen else Res.string.item_complete)
            Checkbox(
                checked = item.done,
                onCheckedChange = onDoneChange,
                enabled = !submitting,
                modifier = Modifier.semantics { contentDescription = completeDescription },
            )
            Icon(
                painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item.checklist?.let { checklist ->
            OrbitProgress(
                label = stringResource(Res.string.module_checklist_title),
                value = stringResource(Res.string.space_item_checklist_progress, checklist.done, checklist.total),
                progress = checklist.progress,
            )
        }
        if (item.hasFooter) {
            OrbitInset(Modifier.fillMaxWidth()) {
                item.assignmentLabel?.let { fallback ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val initials = item.assigneeInitials
                        if (initials != null) {
                            OrbitAvatar(initials = initials, accent = MaterialTheme.colorScheme.primary)
                        } else {
                            Icon(
                                painterResource(Res.drawable.ic_person),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = item.assigneeName ?: stringResource(fallback),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } ?: Spacer(Modifier.weight(1f))
                item.dueDate?.let { date ->
                    Icon(
                        painterResource(Res.drawable.ic_event),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(itemDateLabel(date), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
