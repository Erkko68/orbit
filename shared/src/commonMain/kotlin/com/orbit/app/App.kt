package com.orbit.app

import androidx.compose.runtime.Composable
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.navigation.OrbitNavHost

@Composable
fun App() {
    OrbitTheme {
        OrbitNavHost()
    }
}
