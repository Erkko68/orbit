package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitTransferRowTest : ComposeTest() {
    @Test
    fun alignsTheAmountWithTheActionAndOnlyTheActionIsClickable() = runComposeUiTest {
        var clicks = 0
        setContent {
            OrbitTheme {
                OrbitTransferRow(
                    "Ana",
                    "Marco",
                    "Who pays",
                    "Who receives",
                    "12,40 €",
                    "Mark as paid",
                    { clicks++ },
                    Modifier.width(360.dp)
                )
            }
        }
        onNode(hasText("Ana") and hasText("Marco")).assertIsDisplayed().assertHasNoClickAction()
        val amount = onNodeWithText("12,40 €").fetchSemanticsNode().boundsInRoot
        val button = onNodeWithText("Mark as paid").fetchSemanticsNode().boundsInRoot
        assertTrue(abs(amount.center.y - button.center.y) <= 1f)
        assertTrue(amount.right < button.left)
        onNodeWithText("Mark as paid").assertHeightIsEqualTo(56.dp).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun translatedLargeTextStaysReadableWithoutEnablingADisabledPayment() = runComposeUiTest {
        var clicks = 0
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.5f)) {
                OrbitTheme {
                    OrbitTransferRow(
                        "Alessandra",
                        "Valerio",
                        "Chi paga",
                        "Chi riceve",
                        "123,45 €",
                        "Segna come pagato",
                        { clicks++ },
                        Modifier.width(320.dp),
                        enabled = false
                    )
                }
            }
        }
        onNodeWithText("123,45 €").assertIsDisplayed()
        onNodeWithText("Segna come pagato").assertIsDisplayed().assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
        val amount = onNodeWithText("123,45 €").fetchSemanticsNode().boundsInRoot
        val button = onNodeWithText("Segna come pagato").fetchSemanticsNode().boundsInRoot
        assertTrue(amount.right <= button.left || amount.bottom <= button.top, "Amount=$amount, button=$button")
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("Segna come pagato", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        // Robolectric's legacy text renderer uses placeholder glyph widths; check vertical clipping
        // and actual component bounds here. Real font rendering is verified on the device.
        assertFalse(layout.didOverflowHeight)
        assertTrue(button.right <= onRoot().fetchSemanticsNode().boundsInRoot.right)
    }
}
