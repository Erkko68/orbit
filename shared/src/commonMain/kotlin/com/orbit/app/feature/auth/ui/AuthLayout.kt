package com.orbit.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.component.OrbitCard
import com.orbit.app.core.designsystem.component.OrbitLabeledDivider
import com.orbit.app.core.designsystem.component.OrbitScreen
import com.orbit.app.core.designsystem.component.OrbitScreenHeader
import com.orbit.app.core.designsystem.component.OrbitTextButton
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.or
import orbit.shared.generated.resources.sign_in_google
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What sign-in and create account share: the Orbit header on the page, then one panel with the [form], the other ways
 * in and a line that leads to the other screen: [switchPrompt] and a [switchAction] button that calls [onSwitch].
 */
@Composable
internal fun AuthLayout(
    title: StringResource,
    subtitle: StringResource,
    switchPrompt: StringResource,
    switchAction: StringResource,
    onSwitch: () -> Unit,
    onGoogleSignIn: () -> Unit,
    googleSignInEnabled: Boolean,
    form: @Composable ColumnScope.() -> Unit,
) {
    OrbitScreen(scrollable = true) {
        OrbitScreenHeader(stringResource(title), stringResource(subtitle), Modifier.fillMaxWidth())
        OrbitCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(OrbitSpacing.md), content = form)
            OrbitLabeledDivider(stringResource(Res.string.or))
            OrbitTonalButton(
                text = stringResource(Res.string.sign_in_google),
                onClick = onGoogleSignIn,
                modifier = Modifier.fillMaxWidth(),
                enabled = googleSignInEnabled,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(switchPrompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OrbitTextButton(stringResource(switchAction), onSwitch)
            }
        }
    }
}
