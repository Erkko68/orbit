package com.orbit.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitOutlinedButton
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitScreenHeader
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.sign_in_apple
import orbit.shared.generated.resources.sign_in_google
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What sign-in and create account share, as one centered column: the Orbit header, the [form], the other
 * ways in, then a line that leads to the other screen: [switchPrompt] and a [switchAction] button that
 * calls [onSwitch]. Google and Apple are disabled placeholders until their issues land.
 */
@Composable
internal fun AuthLayout(
    title: StringResource,
    subtitle: StringResource,
    switchPrompt: StringResource,
    switchAction: StringResource,
    onSwitch: () -> Unit,
    form: @Composable ColumnScope.() -> Unit,
) {
    OrbitScreen(
        verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        scrollable = true,
    ) {
        OrbitScreenHeader(stringResource(title), stringResource(subtitle))
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md), content = form)
        OrbitLabeledDivider(stringResource(Res.string.or))
        Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md)) {
            OrbitOutlinedButton(
                text = stringResource(Res.string.sign_in_google),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
            OrbitOutlinedButton(
                text = stringResource(Res.string.sign_in_apple),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(switchPrompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onSwitch) {
                Text(stringResource(switchAction))
            }
        }
    }
}
