package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_event
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OrbitPillTest : ComposeTest() {
    @Test
    fun showsItsLabelInEveryToneWithoutBeingClickable() = runComposeUiTest {
        setContent {
            OrbitTheme {
                Column { OrbitPillTone.entries.forEach { tone -> OrbitPill(tone.name, tone = tone) } }
            }
        }
        OrbitPillTone.entries.forEach { tone ->
            onNodeWithText(tone.name).assertIsDisplayed().assertHasNoClickAction()
        }
    }

    @Test
    fun showsItsLabelNextToAnIcon() = runComposeUiTest {
        setContent { OrbitTheme { OrbitPill("Sunday dinner", icon = Res.drawable.ic_event) } }
        onNodeWithText("Sunday dinner").assertIsDisplayed()
    }
}
