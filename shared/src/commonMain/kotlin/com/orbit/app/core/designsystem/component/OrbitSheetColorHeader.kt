package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_close

private const val TITLE_LINES = 3

/**
 * The top of an [OrbitSheet] about one thing, such as a member: the sheet's top edge in the space's vivid colour, with
 * its planet, the way an [OrbitColorHeader] grows from the top of a screen. Pass it as the sheet's `header`. It has a way
 * back when there is [onBack], [label], what closes the sheet, a [leading] face, a big [title], a [detail] line and a
 * row of [actions]. Text on it is always ink.
 */
@Composable
fun OrbitSheetColorHeader(
    title: String,
    label: String,
    closeDescription: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String? = null,
    onBack: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    detail: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = HeaderCorner, bottomEnd = HeaderCorner),
        color = OrbitTheme.accent.vivid,
        contentColor = Ink,
    ) {
        Column(
            Modifier.headerPlanet().padding(start = OrbitSpacing.xs, top = OrbitSpacing.md, end = OrbitSpacing.xs, bottom = OrbitSpacing.lg),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null && backDescription != null) {
                    OrbitIconButton(Res.drawable.ic_arrow_back, backDescription, onBack)
                }
                Text(
                    text = label,
                    modifier = Modifier.weight(1f).padding(start = if (onBack == null) OrbitSpacing.md else OrbitSpacing.xs),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                OrbitIconButton(Res.drawable.ic_close, closeDescription, onClose)
            }
            Column(
                modifier = Modifier.padding(start = OrbitSpacing.sm + OrbitSpacing.xs, end = OrbitSpacing.md),
                verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            ) {
                leading?.invoke()
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.displayLarge,
                    maxLines = TITLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                detail?.let { Text(it, style = OrbitTheme.data.small) }
                actions?.let {
                    FlowRow(
                        modifier = Modifier.padding(top = OrbitSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.sm),
                        content = { it() },
                    )
                }
            }
        }
    }
}
