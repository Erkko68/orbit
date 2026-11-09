package com.orbit.app.feature.spacehome.expenses

import android.graphics.Bitmap
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.orbit.app.core.designsystem.component.*
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.ic_wallet
import java.io.File
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
                        OrbitSectionHeader("Saldi", Modifier.fillMaxWidth())
                        OrbitAmountRow("Alessandra Maria", "Deve ricevere", "123,45 €", Modifier.fillMaxWidth())
                        OrbitSectionHeader("Salda i conti", Modifier.fillMaxWidth())
                        OrbitTransferRow(
                            listOf(
                                OrbitFace("v", "Valerio Bianchi", "VB", Color.Red, OrbitShape.Circle),
                                OrbitFace("a", "Alessandra Maria", "AM", Color.Cyan, OrbitShape.Circle),
                            ),
                            "Devi a Alessandra Maria",
                            "123,45 €",
                            Modifier.fillMaxWidth(),
                            action = "Paga",
                            actionIcon = Res.drawable.ic_wallet,
                            actionDescription = "Segna come pagato",
                        )
                        OrbitSectionHeader("Registro", Modifier.fillMaxWidth())
                        OrbitAmountRow(
                            "Spesa al supermercato",
                            "Pagato da Alessandra Maria · 7 nov 2026",
                            "123,45 €",
                            Modifier.fillMaxWidth(),
                            onClick = {},
                        )
                    }
                }
            }
        }
        onNodeWithContentDescription("Segna come pagato").performScrollTo().assertIsDisplayed()
        val file = File(context.getExternalFilesDir(null), "$imageName.png")
        file.outputStream()
            .use { onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText("Devi a Alessandra Maria", useUnmergedTree = true)
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
        val check = onNodeWithContentDescription("Segna come pagato").fetchSemanticsNode().boundsInRoot
        val amount = onAllNodesWithText("123,45 €", useUnmergedTree = true)[1].fetchSemanticsNode().boundsInRoot
        assertTrue(amount.right <= check.left, "The amount never runs under the action")
        assertTrue(check.right <= onRoot().fetchSemanticsNode().boundsInRoot.right)
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
