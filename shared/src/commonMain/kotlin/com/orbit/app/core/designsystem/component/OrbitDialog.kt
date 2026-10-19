package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing

/**
 * A short task over the screen, such as adding a space: a [title], the [content] (usually one or two
 * fields) and a way out. Give it [confirmText] and [onConfirm] when the task ends with one action;
 * leave them out when the content has its own buttons. Whether it is open is the caller's state:
 * show it while it is, and close it in [onDismiss], which also runs when the user taps outside or
 * goes back.
 */
@Composable
fun OrbitDialog(
    title: String,
    dismissText: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            if (confirmText != null) {
                TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmText) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
        modifier = modifier,
        title = { Text(title) },
        text = {
            // Fields stand out from the dialog fill the way they do on a card.
            CompositionLocalProvider(LocalInsideCard provides true, LocalCardElevated provides true) {
                // Scrolls when the content outgrows a small screen or the open keyboard.
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md),
                    content = content,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceBright,
    )
}
