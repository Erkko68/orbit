package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.designsystem.ComposeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitTurnCardTest : ComposeTest() {
    @Test
    fun theCardOpensAndItsButtonActs() = runComposeUiTest {
        var opens = 0
        var actions = 0
        setContent {
            OrbitTheme {
                OrbitTurnCard(
                    label = "Today",
                    title = "Bins",
                    actionText = "Done",
                    onAction = { actions++ },
                    onOpen = { opens++ },
                    initials = "LC",
                )
            }
        }

        onNodeWithText("TODAY").performClick()
        assertEquals(1, opens)
        onNodeWithText("Done").performClick()
        assertEquals(1, actions)
        assertEquals(1, opens)
    }
}
