package com.orbit.app.testing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.navigation.OrbitNavHost

/** A launcher in the test APK: the app's real navigation and screens with test-only repositories. */
class ItemManualTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        (application as ItemManualTestApplication).prepareFixtures()
        setContent { OrbitTheme { OrbitNavHost() } }
    }
}
