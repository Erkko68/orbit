package com.orbit.app.core.designsystem.component

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.SemanticsProperties

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

    @Test
    fun alignsTheTwoTextRowsAndEmphasisesOnlyThePayer() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitAmountRow("Food", "Paid by: Ana", "12,40 €", "4 Nov 2026", {}, supportingEmphasis = "Ana")
            }
        }
        val title = onNodeWithText("Food", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val date = onNodeWithText("4 Nov 2026", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val payerNode = onNodeWithText("Paid by: Ana", useUnmergedTree = true).fetchSemanticsNode()
        val amount = onNodeWithText("12,40 €", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(abs(date.right - amount.right) <= 1f)
        assertTrue(abs(title.left - payerNode.boundsInRoot.left) <= 1f)
        assertTrue(title.bottom <= payerNode.boundsInRoot.top)
        val text = payerNode.config[SemanticsProperties.Text].single()
        assertTrue(text.spanStyles.any { it.start == 9 && it.end == 12 && it.item.fontWeight == FontWeight.Bold })
    }
}
