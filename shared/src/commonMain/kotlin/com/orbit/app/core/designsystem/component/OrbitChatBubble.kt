package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitSpacing

private val BubbleMaxWidth = 320.dp
private val SpinnerSize = 20.dp
private val SpinnerStroke = 3.dp

/**
 * One message of a chat, such as the assistant's interview. The other side's ([mine] false) is a panel on the left,
 * the user's a blue bubble on the right, each with a tight corner on its side. [content] is the message, a `Text`
 * already in body type and the bubble's ink, then anything that answers it, such as chips. [loading] puts a spinner
 * before it, for a message still being written. Full width, so the bubble lines up on its side.
 */
@Composable
fun OrbitChatBubble(
    mine: Boolean,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val corner = MaterialTheme.shapes.extraSmall.topStart
    val shape = if (mine) {
        MaterialTheme.shapes.large.copy(bottomEnd = corner)
    } else {
        MaterialTheme.shapes.large.copy(bottomStart = corner)
    }
    Box(modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            modifier = Modifier.widthIn(max = BubbleMaxWidth).then(if (mine) Modifier else Modifier.panelShadow(shape)),
            shape = shape,
            color = if (mine) colors.secondaryContainer else colors.surfaceBright,
            contentColor = if (mine) colors.onSecondaryContainer else colors.onSurface,
        ) {
            CompositionLocalProvider(LocalInsideCard provides true) {
                ProvideTextStyle(MaterialTheme.typography.bodyLarge) {
                    Row(
                        modifier = Modifier.padding(OrbitSpacing.md),
                        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (loading) CircularProgressIndicator(Modifier.size(SpinnerSize), strokeWidth = SpinnerStroke)
                        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md), content = content)
                    }
                }
            }
        }
    }
}
