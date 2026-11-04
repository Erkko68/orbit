package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OrbitAmountRowTest : ComposeTest() {
    @Test
    fun showsTitleTotalPayerAndDateAndOpensIt() = runComposeUiTest {
        var opened = 0
        setContent {
            OrbitTheme {
                OrbitAmountRow(
                    title = "Supermarket",
                    supporting = "Paid by: Ana",
                    amount = "12,40 € — Total",
                    date = "Date: 4 Nov 2026",
                    onClick = { opened++ },
                )
            }
        }
        onNodeWithText("Supermarket").assertIsDisplayed()
        onNodeWithText("12,40 € — Total").assertIsDisplayed()
        onNodeWithText("Paid by: Ana").assertIsDisplayed()
        onNodeWithText("Date: 4 Nov 2026").assertIsDisplayed()
        onNodeWithText("Supermarket").performClick()
        assertEquals(1, opened)
    }
}
