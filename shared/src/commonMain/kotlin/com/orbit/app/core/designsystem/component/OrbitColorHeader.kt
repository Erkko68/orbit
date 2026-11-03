package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_edit
import org.jetbrains.compose.resources.painterResource

internal val HeaderCorner = 32.dp
private val PlanetRadius = 180.dp
private val PlanetRight = 150.dp
private val PlanetTop = 130.dp
private val HaloWidth = 36.dp
private const val PLANET_ALPHA = 0.07f
private const val HALO_ALPHA = 0.04f
private const val TITLE_LINES = 3
private val EditHintSize = 20.dp
private const val EDIT_HINT_ALPHA = 0.6f

/**
 * The top of a screen about one thing, such as a list or a chore: a block of the space's vivid colour under the system
 * bar, with a way back, [label], a [trailing] action, a [leading] face, a big [title] and, when set, a [summary] under
 * it, a wave for [progress], a [detail] line and a row of [actions]. With [onTitleChange] the title is typed in place, [titleDescription] names
 * it, [titlePlaceholder] stands in while it is empty, [focusTitle] puts the cursor there at once and [onTitleDone] says
 * when the user is through. [floating] sits over all of it, for things that move around the block. Pass it to
 * [OrbitScreen]'s `topBar`. Text on it is always ink.
 */
@Composable
fun OrbitColorHeader(
    title: String,
    label: String,
    backDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    summary: (@Composable () -> Unit)? = null,
    detail: String? = null,
    progress: Float? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onTitleChange: ((String) -> Unit)? = null,
    onTitleDone: () -> Unit = {},
    titleDescription: String = title,
    titlePlaceholder: String = titleDescription,
    focusTitle: Boolean = false,
    actions: (@Composable () -> Unit)? = null,
    floating: (@Composable BoxScope.() -> Unit)? = null,
) {
    val insets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = HeaderCorner, bottomEnd = HeaderCorner),
        color = OrbitTheme.accent.vivid,
        contentColor = Ink,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .headerPlanet()
                    .windowInsetsPadding(insets)
                    .padding(start = OrbitSpacing.xs, end = OrbitSpacing.md, bottom = OrbitSpacing.lg),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OrbitIconButton(Res.drawable.ic_arrow_back, backDescription, onBack)
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    trailing?.invoke()
                }
                Column(
                    modifier = Modifier.padding(start = OrbitSpacing.sm + OrbitSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
                ) {
                    leading?.invoke()
                    if (onTitleChange == null) {
                        Text(
                            text = title,
                            modifier = Modifier.semantics { heading() },
                            style = MaterialTheme.typography.displayLarge,
                            maxLines = TITLE_LINES,
                        )
                    } else {
                        HeaderTitleField(title, onTitleChange, onTitleDone, titleDescription, titlePlaceholder, focusTitle)
                    }
                    summary?.invoke()
                    progress?.let { OrbitWavyProgress(it, Modifier.fillMaxWidth().padding(top = OrbitSpacing.sm)) }
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
            floating?.let { Box(Modifier.matchParentSize(), content = it) }
        }
    }
}

/** The faint planet that rises past the top right corner of a block of colour, with its halo. */
internal fun Modifier.headerPlanet(): Modifier = drawBehind {
    val radius = PlanetRadius.toPx()
    val center = Offset(size.width + PlanetRight.toPx() - radius, radius - PlanetTop.toPx())
    drawCircle(Ink.copy(alpha = HALO_ALPHA), radius + HaloWidth.toPx(), center)
    drawCircle(Ink.copy(alpha = PLANET_ALPHA), radius, center)
}

/**
 * The title typed in place, with a pencil standing on its last line that says so until the field has the focus. The
 * keyboard's Done or leaving the field ends it, and a new line is never part of a title.
 */
@Composable
private fun HeaderTitleField(
    title: String,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
    description: String,
    placeholder: String,
    focusOnStart: Boolean,
) {
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }
    val field = rememberFieldValue(title)
    LaunchedEffect(Unit) {
        if (focusOnStart) requester.requestFocus()
    }
    Row(horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.sm)) {
        BasicTextField(
            value = field.value,
            onValueChange = {
                if ('\n' in it.text) {
                    focus.clearFocus()
                } else {
                    field.value = it
                    onChange(it.text)
                }
            },
            modifier = Modifier
                .alignBy(LastBaseline)
                .weight(1f, fill = false)
                .width(IntrinsicSize.Max)
                .focusRequester(requester)
                .onFocusChanged {
                    editing = it.isFocused
                    if (!it.isFocused) onDone()
                }
                .semantics {
                    heading()
                    contentDescription = description
                },
            textStyle = MaterialTheme.typography.displayLarge.copy(color = Ink),
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            maxLines = TITLE_LINES,
            decorationBox = { inner ->
                Box {
                    if (field.value.text.isEmpty()) {
                        Text(placeholder, Modifier.alpha(EDIT_HINT_ALPHA), style = MaterialTheme.typography.displayLarge)
                    }
                    inner()
                }
            },
        )
        if (!editing) {
            Icon(
                painter = painterResource(Res.drawable.ic_edit),
                contentDescription = null,
                modifier = Modifier
                    .alignBy { it.measuredHeight }
                    .size(EditHintSize)
                    .alpha(EDIT_HINT_ALPHA)
                    .clickable { requester.requestFocus() },
            )
        }
    }
}
