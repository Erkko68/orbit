package com.orbit.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.navigation.OrbitNavHost
import com.orbit.app.data.firebase.firebaseSmokeCheck

@Composable
fun App() {
    // Temporary (#6): remove with firebaseSmokeCheck in #15.
    LaunchedEffect(Unit) { firebaseSmokeCheck() }
    OrbitTheme {
        OrbitNavHost()
    }
}
