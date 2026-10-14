package com.orbit.app.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitAvatarButtonTest : ComposeTest() {
    @Test
    fun exposesTheAccountActionAndKeepsAMinimumTouchTarget() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitAvatarButton("AL", MaterialTheme.colorScheme.primary, "Account", onClick = { clicks++ })
            }
        }
        onNodeWithText("AL").assertIsDisplayed()
        onNodeWithContentDescription("Account")
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performClick()
        assertEquals(1, clicks)
    }
}
