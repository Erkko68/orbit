package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.Ink
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.orbit_brand
import org.jetbrains.compose.resources.painterResource

/**
 * The logo on navy, while the session is read: the same picture as the system's startup screen on Android and iOS, so
 * nothing flashes between that screen and the first one of the app.
 */
@Composable
fun OrbitStartup(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        Image(painterResource(Res.drawable.orbit_brand), contentDescription = null)
    }
}
