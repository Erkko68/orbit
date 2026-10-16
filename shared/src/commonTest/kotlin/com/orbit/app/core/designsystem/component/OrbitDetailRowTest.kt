package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import orbit.shared.generated.resources.ic_home
import orbit.shared.generated.resources.Res

@OptIn(ExperimentalTestApi::class)
class OrbitDetailRowTest : ComposeTest() {
    @Test
    fun eventMetadataAlsoSupportsAnInlineValue() = runComposeUiTest {
        setContent { OrbitTheme { OrbitDetailRow(Res.drawable.ic_home, "Space", "Flat 4B", inlineValue = true) } }
        onNodeWithText("Space").assertIsDisplayed()
        onNodeWithText("Flat 4B").assertIsDisplayed()
    }

    @Test
    fun showsTheMetadataLabelAndValue() = runComposeUiTest {
        setContent { OrbitTheme { OrbitDetailRow(Res.drawable.ic_home, "Space", "Flat 4B") } }
        onNodeWithText("Space").assertIsDisplayed()
        onNodeWithText("Flat 4B").assertIsDisplayed()
    }
}
