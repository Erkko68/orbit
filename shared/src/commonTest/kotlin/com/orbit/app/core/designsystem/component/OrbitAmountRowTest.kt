package com.orbit.app.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitAmountRowTest : ComposeTest() {
    @Test
    fun showsTitleOverSupportingWithTheAmountAtTheEndAndOpensIt() = runComposeUiTest {
        var opened = 0
        setContent {
            OrbitTheme {
                OrbitAmountRow("Supermarket", "Paid by Ana · 4 Nov 2026", "12,40 €", onClick = { opened++ })
            }
        }
        val title = onNodeWithText("Supermarket", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val supporting = onNodeWithText("Paid by Ana · 4 Nov 2026", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val amount = onNodeWithText("12,40 €", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(abs(title.left - supporting.left) <= 1f)
        assertTrue(title.bottom <= supporting.top)
        assertTrue(supporting.right <= amount.left)
        onNode(hasText("Supermarket") and hasText("12,40 €")).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun withoutAnActionItIsNotClickableAndShowsItsLeadingFace() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitAmountRow("Ana", "Gets back", "8,00 €", leading = { Text("A") })
            }
        }
        onNode(hasText("Ana") and hasText("Gets back") and hasText("8,00 €") and hasText("A"))
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }

    @Test
    fun aSignedAmountStillReadsAsOneRow() = runComposeUiTest {
        setContent {
            OrbitTheme {
                OrbitAmountRow("Bea", "Owes", "8,00 €", sign = OrbitAmountSign.Minus)
            }
        }
        onNode(hasText("Bea") and hasText("Owes") and hasText("8,00 €")).assertIsDisplayed()
    }
}
