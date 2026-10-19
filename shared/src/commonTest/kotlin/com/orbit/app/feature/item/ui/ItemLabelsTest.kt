package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class ItemLabelsTest : ComposeTest() {
    @Test
    fun wordsADurationInHoursAndMinutes() = runComposeUiTest {
        setContent {
            OrbitTheme {
                Column {
                    Text(itemDurationLabel(15.minutes))
                    Text(itemDurationLabel(120.minutes))
                    Text(itemDurationLabel(90.minutes))
                }
            }
        }
        onNodeWithText("15 min").assertIsDisplayed()
        onNodeWithText("2 h").assertIsDisplayed()
        onNodeWithText("1 h 30 min").assertIsDisplayed()
    }
}
