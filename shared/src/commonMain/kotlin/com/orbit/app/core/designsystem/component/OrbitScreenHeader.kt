package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

/** Opens a screen that has no top bar, such as sign-in: the Orbit name, then what the screen is for. */
@Composable
fun OrbitScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(OrbitSpacing.lg)) {
        Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleLarge)
        OrbitHeading(title, subtitle, titleStyle = MaterialTheme.typography.headlineMedium)
    }
}
