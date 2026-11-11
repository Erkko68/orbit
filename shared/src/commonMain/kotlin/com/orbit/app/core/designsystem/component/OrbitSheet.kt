package com.orbit.app.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.PanelTopShape
import kotlinx.coroutines.launch
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_close

/**
 * A task over the screen, the way Material 3 does it on a phone: a sheet that rises from the bottom edge, as tall as its
 * content, and closes with a swipe, a tap outside, back or the close button. Whether it is open is the caller's state:
 * show it while it is, and close it in [onDismiss]. With [confirmText] it ends in one action, in red when [danger].
 * A short task of several steps, such as picking a date and then a time, stays in one sheet: pass [onBack] on the
 * steps after the first, and show them in [OrbitSheetPages], so the sheet changes in place and never opens a second
 * one on top. A section of the app is a screen of its own, never a sheet with pages.
 */
// M3's ModalBottomSheet is experimental in material3 1.9.0: an exception AGENTS.md allows, here and nowhere else.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitSheet(
    title: String,
    closeDescription: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String? = null,
    onBack: (() -> Unit)? = null,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    confirmEnabled: Boolean = true,
    danger: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = state,
        shape = PanelTopShape,
        containerColor = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null,
    ) {
        CompositionLocalProvider(LocalInsideCard provides true) {
            Column(Modifier.imePadding().verticalScroll(rememberScrollState())) {
                Box {
                    SheetTitle(title, closeDescription, close, backDescription, onBack)
                    SheetHandle(Modifier.align(Alignment.TopCenter))
                }
                Column(
                    modifier = Modifier.padding(OrbitSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                ) {
                    content()
                    if (confirmText != null) {
                        OrbitButton(confirmText, onConfirm, Modifier.fillMaxWidth(), confirmEnabled, danger)
                    }
                }
            }
        }
    }
}

/** What says the sheet can be swiped, on its top edge. */
@Composable
private fun SheetHandle(modifier: Modifier) {
    Box(
        modifier
            .padding(top = OrbitSpacing.sm)
            .size(HandleWidth, HandleHeight)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HANDLE_ALPHA), CircleShape),
    )
}

@Composable
private fun SheetTitle(
    title: String,
    closeDescription: String,
    onClose: () -> Unit,
    backDescription: String?,
    onBack: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.padding(start = OrbitSpacing.md, top = OrbitSpacing.md, end = OrbitSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null && backDescription != null) {
            OrbitIconButton(Res.drawable.ic_arrow_back, backDescription, onBack)
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        OrbitIconButton(icon = Res.drawable.ic_close, contentDescription = closeDescription, onClick = onClose)
    }
}

/**
 * The steps of a task inside one [OrbitSheet]. Going to another [page] slides its content in, and the sheet grows or
 * shrinks to fit it.
 */
@Composable
fun <T> OrbitSheetPages(page: T, modifier: Modifier = Modifier, content: @Composable ColumnScope.(T) -> Unit) {
    AnimatedContent(
        targetState = page,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(OrbitMotion.effects()) + slideInHorizontally(OrbitMotion.spatial()) { it / SLIDE_FRACTION }) togetherWith
                fadeOut(OrbitMotion.effects())
        },
        contentAlignment = Alignment.TopStart,
    ) { shown ->
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) { content(shown) }
    }
}

private const val SLIDE_FRACTION = 6
private const val HANDLE_ALPHA = 0.4f
private val HandleWidth = 32.dp
private val HandleHeight = 4.dp
