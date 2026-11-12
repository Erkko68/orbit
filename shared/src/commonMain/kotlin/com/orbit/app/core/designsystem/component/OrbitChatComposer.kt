package com.orbit.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_send
import org.jetbrains.compose.resources.painterResource

private val SendSize = 48.dp
private const val MAX_LINES = 4

// The send button is a circle that squares off a little while pressed, as M3 Expressive buttons do.
private const val ROUND = 50
private const val PRESSED_ROUND = 30

/**
 * The bottom of a chat, for [OrbitScreen]'s `bottomBar`: one soft pill that holds the message, up to four lines, and
 * a round send button in the accent, which [canSend] lights up. It rides above the keyboard. [enabled] is the
 * field's; [trailingIcon] sits before the send button, such as a microphone.
 */
@Composable
fun OrbitChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    sendDescription: String,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canSend: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val field = rememberFieldValue(value)
    Box(
        modifier
            .fillMaxWidth()
            .background(OrbitTheme.accent.page)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = OrbitSpacing.md, vertical = OrbitSpacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ControlHeight)
                .panelShadow(shape)
                .background(colors.surfaceBright, shape)
                .padding(start = OrbitSpacing.md, end = OrbitSpacing.xs)
                .padding(vertical = OrbitSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            BasicTextField(
                value = field.value,
                onValueChange = {
                    field.value = it
                    onValueChange(it.text)
                },
                modifier = Modifier.weight(1f).heightIn(min = SendSize),
                enabled = enabled,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                maxLines = MAX_LINES,
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { text ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (field.value.text.isEmpty()) {
                            val style = MaterialTheme.typography.bodyLarge
                            Text(placeholder, style = style, color = colors.onSurfaceVariant)
                        }
                        text()
                    }
                },
            )
            trailingIcon?.invoke()
            SendButton(sendDescription, onSend, canSend)
        }
    }
}

@Composable
private fun SendButton(contentDescription: String, onClick: () -> Unit, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val round by animateIntAsState(if (pressed) PRESSED_ROUND else ROUND, OrbitMotion.spatial())
    val fill by animateColorAsState(
        if (enabled) OrbitTheme.accent.vivid else colors.surfaceContainer,
        OrbitMotion.effects(),
    )
    val ink by animateColorAsState(if (enabled) Ink else colors.onSurfaceVariant, OrbitMotion.effects())
    Box(
        modifier = Modifier
            .size(SendSize)
            .clip(RoundedCornerShape(percent = round))
            .background(fill)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(Res.drawable.ic_send), contentDescription, tint = ink)
    }
}
