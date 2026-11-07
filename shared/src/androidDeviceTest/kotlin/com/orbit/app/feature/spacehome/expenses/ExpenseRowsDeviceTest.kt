package com.orbit.app.feature.spacehome.expenses

import android.graphics.Bitmap
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.orbit.app.core.designsystem.component.*
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_arrow_back
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Native Android font rendering, with test-only values and no repository or Firebase access. */
@OptIn(ExperimentalTestApi::class)
class ExpenseRowsDeviceTest {
    @Test
    fun normalTextKeepsAmountAndActionAligned() = verifyRows(1f, "spese-normale")

    @Test
    fun enlargedTextKeepsTheWholeActionReadable() = verifyRows(1.5f, "spese-testo-grande")

    private fun verifyRows(fontScale: Float, imageName: String) = runComposeUiTest {
        lateinit var context: Context
        setContent {
            context = LocalContext.current
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                OrbitTheme {
                    OrbitScreen(scrollable = true) {
                        OrbitSectionHeader("Saldi", Modifier.fillMaxWidth(), centered = true)
                        OrbitBalanceRow(
                            "Alessandra Maria",
                            "Da ricevere",
                            "123,45 €",
                            Res.drawable.ic_arrow_back,
                            Modifier.fillMaxWidth()
                        )
                        OrbitSectionHeader("Pagamenti suggeriti", Modifier.fillMaxWidth(), centered = true)
                        OrbitTransferRow(
                            "Alessandra Maria",
                            "Valerio Bianchi",
                            "Chi paga",
                            "Chi riceve",
                            "123,45 €",
                            "Segna come pagato",
                            {},
                            Modifier.fillMaxWidth()
                        )
                        OrbitSectionHeader("Registro", Modifier.fillMaxWidth(), centered = true)
                        OrbitAmountRow(
                            "Spesa al supermercato",
                            "Pagato da: Alessandra Maria",
                            "123,45 €",
                            "7 nov 2026",
                            {},
                            Modifier.fillMaxWidth(),
                            "Alessandra Maria"
                        )
                    }
                }
            }
        }
        onNodeWithText("Segna come pagato").performScrollTo().assertIsDisplayed()
        val file = File(context.getExternalFilesDir(null), "$imageName.png")
        file.outputStream()
            .use { onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("Segna come pagato", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        // A paragraph can retain the maximum measurement width while Text wraps its visible lines.
        // Check the actual glyph bounds, rather than treating that empty paragraph space as clipping.
        for (line in 0 until layout.lineCount) {
            assertFalse(layout.isLineEllipsized(line))
            assertTrue(layout.getLineRight(line) <= layout.size.width + 1f)
            assertTrue(layout.getLineLeft(line) >= -1f)
            assertTrue(layout.getLineBottom(line) <= layout.size.height + 1f)
        }
        val button = onNodeWithText("Segna come pagato").fetchSemanticsNode().boundsInRoot
        val amount = onAllNodesWithText("123,45 €", useUnmergedTree = true)[1].fetchSemanticsNode().boundsInRoot
        assertTrue(amount.right <= button.left || amount.bottom <= button.top)
        if (fontScale == 1f) assertTrue(amount.right <= button.left, "Normal text keeps amount and action on one row")
        if (amount.right <= button.left) assertTrue(abs(amount.center.y - button.center.y) <= 1f)
        val balanceLayouts = mutableListOf<TextLayoutResult>()
        onAllNodesWithText("123,45 €", useUnmergedTree = true)[0]
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(balanceLayouts) }
        assertTrue(balanceLayouts.single().lineCount == 1, "Keep the balance amount together")
        val nameLayouts = mutableListOf<TextLayoutResult>()
        onAllNodesWithText("Alessandra Maria", useUnmergedTree = true)[0]
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(nameLayouts) }
        assertTrue(nameLayouts.single().lineCount <= 2, "A long name must not become a column of fragments")
    }
}
