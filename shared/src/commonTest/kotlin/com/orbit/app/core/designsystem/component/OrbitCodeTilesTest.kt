package com.orbit.app.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitCodeTilesTest : ComposeTest() {
    @Test
    fun readsTheCodeAsOneWord() = runComposeUiTest {
        setContent { OrbitTheme { OrbitCodeTiles("AB12CD34", length = 8, modifier = Modifier) } }

        onNodeWithContentDescription("AB12CD34").assertIsDisplayed()
        onNodeWithText("A").assertDoesNotExist()
    }

    @Test
    fun holdsTheCodesPlaceUntilItArrives() = runComposeUiTest {
        setContent { OrbitTheme { OrbitCodeTiles(null, length = 8) } }

        onNodeWithText("A").assertDoesNotExist()
    }
}
