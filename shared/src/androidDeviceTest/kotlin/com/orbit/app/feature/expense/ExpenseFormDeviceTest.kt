package com.orbit.app.feature.expense

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.core.deviceMoneyLocale
import com.orbit.app.core.moneyInput
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Space
import com.orbit.app.feature.expense.ui.ExpenseScreen
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryExpenseRepository
import kotlinx.coroutines.runBlocking
import orbit.shared.generated.resources.*
import org.jetbrains.compose.resources.getString
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Full form on native Android; repositories and user data exist only in the test process. */
@OptIn(ExperimentalTestApi::class)
class ExpenseFormDeviceTest {
    @Test
    fun normalTextKeepsTheCompactSelectorAndConversionsReadable() = verifyForm(1f, "normal")

    @Test
    fun enlargedTextKeepsTheCompactSelectorAndConversionsReadable() = verifyForm(1.5f, "large")

    private fun verifyForm(fontScale: Float, name: String) = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val names = mapOf("ana" to "Alessandra Maria Bianchi", "bea" to "Beatrice", "marc" to "Marc")
        val spaces = FakeSpaceRepository(
            Space("space-1", "Flat", accent = 0, memberIds = names.keys.toList()),
            names.map { (id, display) -> Member(id, display, null, if (id == "ana") Role.OWNER else Role.MEMBER) },
        )
        val ledger = InMemoryExpenseRepository(auth)
        runBlocking { auth.register("ana", "secret", "Ana") }
        val vm = ExpenseViewModel("space-1", "", ledger, spaces, auth)
        lateinit var context: Context
        setContent {
            context = LocalContext.current
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                OrbitTheme { ExpenseScreen("space-1", "", {}, vm) }
            }
        }
        val amount = runBlocking { getString(Res.string.expense_amount) }
        val custom = runBlocking { getString(Res.string.expense_exact) }
        val weights = runBlocking { getString(Res.string.expense_weights) }
        val percent = runBlocking { getString(Res.string.expense_unit_percent_description) }
        val euros = runBlocking { getString(Res.string.expense_unit_euro_description) }
        val title = runBlocking { getString(Res.string.expense_title) }
        onNodeWithContentDescription(title).performTextReplacement("Groceries")
        onNode(hasSetTextAction() and hasText(amount)).performScrollTo()
            .performTextInput(moneyInput(4000, deviceMoneyLocale()))
        onNodeWithText(custom).performScrollTo().performClick()
        onNodeWithContentDescription(percent).performScrollTo().performClick().assertIsSelected()
        for ((id, value) in listOf("ana" to "50", "bea" to "25", "marc" to "25")) {
            val label = runBlocking { getString(Res.string.expense_share, names.getValue(id)) }
            onNodeWithContentDescription(label).performScrollTo().performTextReplacement(value)
        }
        assertEquals(mapOf("ana" to 2000L, "bea" to 1000L, "marc" to 1000L), vm.uiState.value.split)
        val nameLayouts = mutableListOf<TextLayoutResult>()
        onAllNodesWithText(names.getValue("ana"), useUnmergedTree = true).onLast().performScrollTo()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(nameLayouts) }
        val nameLayout = nameLayouts.single()
        for (line in 0 until nameLayout.lineCount) {
            assertFalse(nameLayout.isLineEllipsized(line), "The participant name must remain readable")
        }
        onNodeWithContentDescription(percent).performScrollTo()
        val layouts = mutableListOf<TextLayoutResult>()
        onAllNodesWithText("%", useUnmergedTree = true).onFirst()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        File(context.getExternalFilesDir(null), "expense-55-percent-$name.png").outputStream().use {
            onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val layout = layouts.single()
        // Measure visible glyph bounds: Android can retain unused paragraph width after a narrower remeasure.
        for (line in 0 until layout.lineCount) {
            assertFalse(layout.isLineEllipsized(line), "The percentage symbol must not be ellipsized")
            assertTrue(
                layout.getLineRight(line) <= layout.size.width + 1f,
                "Percentage exceeds its right edge: $layout"
            )
            assertTrue(layout.getLineLeft(line) >= -1f, "Percentage exceeds its left edge: $layout")
            assertTrue(
                layout.getLineBottom(line) <= layout.size.height + 1f,
                "Percentage exceeds its bottom edge: $layout"
            )
        }
        onNodeWithContentDescription(euros).performScrollTo().performClick().assertIsSelected()
        assertEquals(mapOf("ana" to 2000L, "bea" to 1000L, "marc" to 1000L), vm.uiState.value.split)
        onNodeWithText(weights).performScrollTo().performClick()
        val weightLabel = runBlocking { getString(Res.string.expense_weight_share, names.getValue("ana")) }
        onNodeWithContentDescription(weightLabel).performScrollTo().performTextReplacement("2")
        assertEquals(mapOf("ana" to 2000L, "bea" to 1000L, "marc" to 1000L), vm.uiState.value.split)
        File(context.getExternalFilesDir(null), "expense-55-weights-$name.png").outputStream().use {
            onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
