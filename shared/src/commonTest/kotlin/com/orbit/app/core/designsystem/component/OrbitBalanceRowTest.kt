package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_back
import orbit.shared.generated.resources.ic_arrow_forward
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitBalanceRowTest : ComposeTest() {
    @Test
    fun alignsNamesAmountsAndDirectionsAcrossRowsAndReadsZeroWithoutAnArrow() = runComposeUiTest {
        setContent {
            OrbitTheme {
                Column(Modifier.width(360.dp)) {
                    OrbitBalanceRow("Ana", "To receive", "20,00 €", Res.drawable.ic_arrow_back)
                    OrbitBalanceRow("Beatrice", "To pay", "10,00 €", Res.drawable.ic_arrow_forward)
                    OrbitBalanceRow("Marco", "Settled up", "0,00 €", null)
                }
            }
        }
        onNode(hasText("Marco") and hasText("Settled up")).assertIsDisplayed().assertHasNoClickAction()
        val name = onNodeWithText("Ana", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val amount = onNodeWithText("20,00 €", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val other = onNodeWithText("10,00 €", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val receives = onNodeWithText("To receive", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val pays = onNodeWithText("To pay", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(abs(name.center.y - amount.center.y) <= 1f)
        assertTrue(abs(amount.right - other.right) <= 1f)
        assertTrue(abs(receives.center.x - pays.center.x) <= 1f)
        assertTrue(name.right < receives.left && receives.right < amount.left)
    }

    @Test
    fun longNamesAndLargeTextRemainVisible() = runComposeUiTest {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                OrbitTheme {
                    OrbitBalanceRow(
                        "Alessandra Maria", "Da ricevere", "123,45 €", Res.drawable.ic_arrow_back,
                        Modifier.width(320.dp),
                    )
                }
            }
        }
        onNode(hasText("Alessandra Maria") and hasText("Da ricevere") and hasText("123,45 €")).assertIsDisplayed()
        val name = onNodeWithText("Alessandra Maria", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val status = onNodeWithText("Da ricevere", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val amount = onNodeWithText("123,45 €", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(
            name.bottom <= minOf(
                status.top,
                amount.top
            ) || name.right <= status.left && status.right <= amount.left
        )
    }
}
