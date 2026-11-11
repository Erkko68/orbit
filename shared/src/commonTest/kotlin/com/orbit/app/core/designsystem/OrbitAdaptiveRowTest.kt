package com.orbit.app.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.orbit.app.core.designsystem.component.OrbitAdaptiveRow
import com.orbit.app.core.designsystem.component.OrbitAmountField
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitAdaptiveRowTest : ComposeTest() {
    @Test
    fun english() = verifyRows("Automate")

    @Test
    fun italian() = verifyRows("Automatizza")

    @Test
    fun spanish() = verifyRows("Automatizar")

    @Test
    fun catalan() = verifyRows("Automatitza")

    private fun verifyRows(label: String) = runComposeUiTest {
        val amounts = listOf("40,00", "400,00", "4.000,00", "40.000,00", "1234567890,12", "40,00")
        setContent {
            OrbitTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    for (index in amounts.indices) {
                        val scale = if (index == 4) 1.5f else 1f
                        val density = LocalDensity.current
                        CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                            Box(
                                Modifier.width(
                                    OrbitSpacing.xl * when (index) {
                                        0, 1, 2 -> 8
                                        3 -> 9
                                        4 -> 10
                                        else -> 5
                                    }
                                )
                            ) {
                                OrbitAdaptiveRow(Modifier.fillMaxWidth().testTag("row-$index"), leading = {
                                    OrbitTonalButton(label, {}, Modifier.testTag("action-$index"), compact = true)
                                }, trailing = {
                                    OrbitAmountField(
                                        amounts[index],
                                        {},
                                        "amount-$index",
                                        "0",
                                        suffix = "€"
                                    )
                                })
                            }
                        }
                    }
                }
            }
        }
        for (index in amounts.indices) {
            onNodeWithTag("row-$index").performScrollTo()
            val action = onNodeWithTag("action-$index").fetchSemanticsNode().boundsInRoot
            val field = onNodeWithContentDescription("amount-$index").fetchSemanticsNode().boundsInRoot
            assertTrue(action.right <= field.left || action.bottom <= field.top)
            assertEquals(0f, action.left)
            if (index < 4) assertTrue(
                action.right <= field.left,
                "${amounts[index]} € must fit beside the action at normal text size: $label"
            )
            assertTrue(field.right <= onRoot().fetchSemanticsNode().boundsInRoot.right)
            if (index == 5) assertTrue(
                action.bottom <= field.top,
                "Insufficient width must wrap even with simulated fonts"
            )
            val layouts = mutableListOf<TextLayoutResult>()
            onAllNodesWithText(label, useUnmergedTree = true)[index]
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse(layout.didOverflowHeight)
            assertEquals(label.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
            for (line in 0 until layout.lineCount) {
                assertFalse(layout.isLineEllipsized(line))
                assertTrue(layout.getLineRight(line) <= layout.size.width + 1)
            }
        }
    }
}
