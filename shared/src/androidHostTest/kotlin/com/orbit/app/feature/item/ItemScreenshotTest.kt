package com.orbit.app.feature.item

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemoryItemRepository
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.feature.item.ui.ItemScreen
import com.orbit.app.testing.FakeSpaceRepository
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Local visual evidence for the mockup comparison; the application never receives these fixtures. */
@OptIn(ExperimentalTestApi::class)
@Config(qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ItemScreenshotTest : ComposeTest() {
    private val viewModels = ViewModelStore()

    @AfterTest
    fun tearDown() {
        viewModels.clear()
    }

    @Test
    fun taskLight() = capture(Preset.TASK, false)

    @Test
    fun taskDark() = capture(Preset.TASK, true)

    @Test
    fun listLight() = capture(Preset.SHOPPING_LIST, false)

    @Test
    fun listDark() = capture(Preset.SHOPPING_LIST, true)

    @Test
    fun eventLight() = capture(Preset.EVENT, false)

    @Test
    fun eventDark() = capture(Preset.EVENT, true)

    private fun capture(preset: Preset, dark: Boolean) = runComposeUiTest {
        val auth = InMemoryAuthRepository()
        val items = InMemoryItemRepository(auth)
        val spaces = FakeSpaceRepository(
            com.orbit.app.domain.model.Space("space-1", "Flat 4B", 0, listOf("ana@orbit.app")),
            listOf(com.orbit.app.domain.model.Member("ana@orbit.app", "space-1", "Ana", null, Role.OWNER)),
        )
        val title = when (preset) {
            Preset.TASK -> "Book plumber"
            Preset.SHOPPING_LIST -> "Groceries"
            Preset.EVENT -> "Dinner at 20:00"
        }
        val modules = when (preset) {
            Preset.TASK -> ItemModules(schedule = Schedule(LocalDate(2026, 10, 26)), assignment = Assignment.Anyone)
            Preset.SHOPPING_LIST -> ItemModules(
                checklist = Checklist(
                    listOf(
                        ChecklistEntry("milk", "Milk", true), ChecklistEntry("eggs", "Eggs", true),
                        ChecklistEntry("pasta", "Pasta", false), ChecklistEntry("tomatoes", "Tomatoes", false),
                        ChecklistEntry("coffee", "Coffee", false), ChecklistEntry("soap", "Dish soap", false),
                    )
                )
            )

            Preset.EVENT -> ItemModules(schedule = Schedule(LocalDate(2026, 10, 26), LocalTime(20, 0)))
        }
        val id = runBlocking {
            auth.signIn("ana@orbit.app", "secret")
            items.create(
                "space-1", NewItem(
                    title,
                    description = when (preset) {
                        Preset.TASK -> "Kitchen leak"
                        Preset.EVENT -> "Dinner together at home"
                        Preset.SHOPPING_LIST -> null
                    }, preset = preset, modules = modules
                )
            )
        }
        val viewModel = ItemViewModel("space-1", id, items, spaces).also { viewModels.put("item", it) }
        setContent { OrbitTheme(darkTheme = dark) { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText(title).assertIsDisplayed()
        val directory = File("build/reports/item-screenshots").apply { mkdirs() }
        fun save(suffix: String) {
            val output = File(directory, "${preset.name.lowercase()}-${if (dark) "dark" else "light"}$suffix.png")
            output.outputStream().use { stream ->
                assertTrue(onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
        }
        save("")
        onNodeWithText(if (preset == Preset.SHOPPING_LIST) "+ Assignment" else "+ Reminder").performScrollTo()
        save("-details")
    }
}
