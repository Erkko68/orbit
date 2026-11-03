package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitListTileTest : ComposeTest() {
    @Test
    fun saysWhatIsLeftOnOneLineAndOpens() = runComposeUiTest {
        var opens = 0
        setContent {
            OrbitTheme {
                OrbitListTile("Weekly shop", "2 of 5", 0.4f, { opens++ }, peek = listOf("Oat milk", "Eggs"), peekNote = "+1 more")
            }
        }

        onNodeWithText("Oat milk, Eggs, +1 more").assertIsDisplayed()
        onNodeWithText("2 of 5").assertIsDisplayed()
        onNodeWithText("Weekly shop").performClick()
        assertEquals(1, opens)
    }

    @Test
    fun isOnlyAsTallAsWhatItSays() = runComposeUiTest {
        setContent {
            OrbitTheme {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    OrbitListTile("Weekly shop", "0 of 0", 0f, {}, Modifier.testTag("tile"))
                }
            }
        }

        assertTrue(onNodeWithTag("tile").getUnclippedBoundsInRoot().height < 140.dp)
    }
}
