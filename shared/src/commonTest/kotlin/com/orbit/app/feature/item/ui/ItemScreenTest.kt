package com.orbit.app.feature.item.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.testing.ControlledItemRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class ItemScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository().apply {
        runBlocking { signIn("ana@orbit.app", "secret") }
    }
    private val items = InMemoryItemRepository(auth)
    private val spaces = FakeSpaceRepository(
        com.orbit.app.domain.model.Space("space-1", "Flat 4B", 0, listOf("ana", "mika")),
        listOf(
            com.orbit.app.domain.model.Member("ana", "Ana", null, Role.OWNER),
            com.orbit.app.domain.model.Member("mika", "Mika", null, Role.MEMBER),
        ),
    )
    private val viewModels = ViewModelStore()

    @AfterTest
    fun tearDown() {
        viewModels.clear()
    }

    @Test
    fun rendersItemHeaderAndModuleCards() = runComposeUiTest {
        val spaceId = "space-1"
        val schedule = Schedule(date = LocalDate(2026, 10, 26))
        val checklist = Checklist(listOf(ChecklistEntry("entry-1", "Kitchen faucet", checked = false)))
        val itemId = runBlocking {
            items.create(
                spaceId,
                NewItem(
                    title = "Book plumber",
                    description = "Kitchen leak",
                    preset = Preset.TASK,
                    modules = ItemModules(schedule = schedule, checklist = checklist),
                ),
            )
        }
        val viewModel = model(itemId, items)

        setContent {
            OrbitTheme {
                ItemScreen(spaceId = spaceId, itemId = itemId, onBack = {}, viewModel = viewModel)
            }
        }

        onNodeWithText("Book plumber").assertIsDisplayed()
        onNodeWithText("Kitchen leak").assertIsDisplayed()
        onAllNodesWithText("Task").assertCountEquals(2)
        val presetPill = onAllNodesWithText("Task").fetchSemanticsNodes().maxBy { it.boundsInRoot.top }
        val title = onNodeWithText("Book plumber").fetchSemanticsNode()
        assertTrue(presetPill.boundsInRoot.bottom <= title.boundsInRoot.top)
        onNodeWithText("Schedule").performScrollTo().assertIsDisplayed()
        onNodeWithText("Checklist").performScrollTo().assertIsDisplayed()
        onNodeWithText("Add detail").performScrollTo().assertIsDisplayed()
        onNodeWithText("+ Assignment").performScrollTo().assertIsDisplayed()
        onNodeWithText("+ Reminder").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun addsModuleWhenTappingAddDetailChip() = runComposeUiTest {
        val spaceId = "space-1"
        val itemId = runBlocking {
            items.create(
                spaceId,
                NewItem(title = "Simple Task", preset = Preset.TASK),
            )
        }
        val viewModel = model(itemId, items)

        setContent {
            OrbitTheme {
                ItemScreen(spaceId = spaceId, itemId = itemId, onBack = {}, viewModel = viewModel)
            }
        }

        onNodeWithText("+ Checklist").performScrollTo().performClick()
        onNodeWithText("Checklist").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun removesModuleWhenClickingRemoveIcon() = runComposeUiTest {
        val spaceId = "space-1"
        val checklist = Checklist(listOf(ChecklistEntry("entry-1", "Milk", checked = true)))
        val itemId = runBlocking {
            items.create(
                spaceId,
                NewItem(
                    title = "Groceries",
                    preset = Preset.SHOPPING_LIST,
                    modules = ItemModules(checklist = checklist),
                ),
            )
        }
        val viewModel = model(itemId, items)

        setContent {
            OrbitTheme {
                ItemScreen(spaceId = spaceId, itemId = itemId, onBack = {}, viewModel = viewModel)
            }
        }

        onNodeWithText("Checklist").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Remove checklist").performScrollTo().performClick()
        onNodeWithText("+ Checklist").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun missingItemShowsAnEmptyStateInsteadOfEndlessLoading() = runComposeUiTest {
        val viewModel = model("missing", items)
        setContent { OrbitTheme { ItemScreen("space-1", "missing", {}, viewModel) } }
        onNodeWithText("Item not found").assertIsDisplayed()
        onNodeWithText("Add detail").assertDoesNotExist()
    }

    @Test
    fun loadingStateDoesNotOfferItemActions() = runComposeUiTest {
        val gate = CompletableDeferred<Unit>()
        val repository = ControlledItemRepository(items).apply { beforeRead = { gate.await() } }
        val viewModel = model("missing", repository)
        setContent { OrbitTheme { ItemScreen("space-1", "missing", {}, viewModel) } }
        onNodeWithText("Loading item…").assertIsDisplayed()
        onNodeWithText("Add detail").assertDoesNotExist()
    }

    @Test
    fun readErrorOffersRetryAndCanRecover() = runComposeUiTest {
        val id = createItem(NewItem("Recoverable task", preset = Preset.TASK))
        val repository = ControlledItemRepository(items).apply { beforeRead = { error("Read denied") } }
        val viewModel = model(id, repository)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Could not load item").assertIsDisplayed()
        onNodeWithText("Item not found").assertDoesNotExist()
        repository.beforeRead = {}
        onNodeWithText("Try again").performScrollTo().performClick()
        onNodeWithText("Recoverable task").assertIsDisplayed()
        onNodeWithText("Could not load item").assertDoesNotExist()
    }

    @Test
    fun pendingWriteDisablesChipsAndRemoval() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Pending task", preset = Preset.TASK, modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 10, 26)),
                )
            )
        )
        val gate = CompletableDeferred<Unit>()
        val repository = ControlledItemRepository(items).apply { beforeWrite = { gate.await() } }
        val viewModel = model(id, repository)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("+ Checklist").performScrollTo().performClick()
        onNodeWithText("+ Assignment").performScrollTo().assertIsNotEnabled()
        onNodeWithContentDescription("Remove schedule").performScrollTo().assertIsNotEnabled()
        runOnIdle { gate.complete(Unit) }
        onNodeWithContentDescription("Remove checklist").performScrollTo().assertIsEnabled()
        onNodeWithText("+ Assignment").performScrollTo().assertIsEnabled()
        assertEquals(listOf("set:CHECKLIST"), repository.writes)
    }

    @Test
    fun failedRemovalShowsAnErrorAndTheNextAttemptCanSucceed() = runComposeUiTest {
        val id = createItem(NewItem("Task", modules = ItemModules(checklist = Checklist(emptyList()))))
        val repository = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val viewModel = model(id, repository)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithContentDescription("Remove checklist").performScrollTo().performClick()
        onNodeWithText("Action failed. Try again.").performScrollTo().assertIsDisplayed()
        repository.beforeWrite = {}
        onNodeWithContentDescription("Remove checklist").performScrollTo().performClick()
        onNodeWithText("+ Checklist").performScrollTo().assertIsDisplayed()
        onNodeWithText("Action failed. Try again.").assertDoesNotExist()
    }

    @Test
    fun checklistRendersEntriesAndQuantities() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Groceries", preset = Preset.SHOPPING_LIST, modules = ItemModules(
                    checklist = Checklist(
                        listOf(
                            ChecklistEntry("milk", "Milk", true, 2), ChecklistEntry("bread", "Bread", false),
                        )
                    ),
                )
            )
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Milk × 2").performScrollTo().assertIsDisplayed().assertHasNoClickAction()
        onNodeWithText("Bread").performScrollTo().assertIsDisplayed().assertHasNoClickAction()
        onNodeWithText("+ Reminder").assertDoesNotExist()
    }

    @Test
    fun eventRendersScheduleDurationAndReminderOffsets() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Dinner", preset = Preset.EVENT, modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 10, 26), time = LocalTime(20, 0), duration = 30.minutes),
                    reminder = Reminder(listOf(Duration.ZERO, 10.minutes)),
                    assignment = Assignment.Anyone,
                )
            )
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Oct 26, 2026 · 20:00").performScrollTo().assertIsDisplayed()
        onNodeWithText("Duration: 30m").performScrollTo().assertIsDisplayed()
        onNodeWithText("Anyone").performScrollTo().assertIsDisplayed()
        onNodeWithText("At event time, 10m before").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun topBarShowsTheTypeAndSpace() = runComposeUiTest {
        val id = createItem(NewItem("Dinner", preset = Preset.EVENT))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onAllNodesWithText("Event").assertCountEquals(2)
        onNodeWithText("Flat 4B").assertIsDisplayed()
    }

    @Test
    fun dateRangeAndUnassignedClaimAreRendered() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Window", modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 10, 26), endDate = LocalDate(2026, 10, 28)),
                    assignment = Assignment.Claim(),
                    reminder = Reminder(emptyList()),
                )
            )
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Oct 26, 2026 – Oct 28, 2026").performScrollTo().assertIsDisplayed()
        onNodeWithText("Unassigned").performScrollTo().assertIsDisplayed()
        onNodeWithText("No reminders set").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun memberAndClaimAssignmentsDisplayTheStoredAssignee() = runComposeUiTest {
        val id = createItem(NewItem("Assigned task", modules = ItemModules(assignment = Assignment.ToMember("ana"))))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Ana").performScrollTo().assertIsDisplayed()
        onNodeWithText("A").performScrollTo().assertIsDisplayed()
        runOnIdle { runBlocking { items.setModule("space-1", id, Assignment.Claim("mika")) } }
        onNodeWithText("Mika").performScrollTo().assertIsDisplayed()
        onNodeWithText("M").performScrollTo().assertIsDisplayed()
        onNodeWithText("A").assertDoesNotExist()
    }

    @Test
    fun darkThemeKeepsMissingMemberAndScheduleReadable() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Shared task",
                modules = ItemModules(
                    assignment = Assignment.ToMember("missing"),
                    schedule = Schedule(LocalDate(2026, 10, 26), time = LocalTime(20, 0)),
                ),
            ),
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme(darkTheme = true) { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithText("Oct 26, 2026 · 20:00").performScrollTo().assertIsDisplayed()
        onNodeWithText("Unknown member").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Remove assignment").performScrollTo().assertIsEnabled()
    }

    @Test
    fun removingScheduleRemovesReminderAndRestoresOnlyValidChips() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Reminder task", modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 10, 26)), reminder = Reminder(listOf(Duration.ZERO)),
                )
            )
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithContentDescription("Remove schedule").performScrollTo().performClick()
        onNodeWithText("+ Schedule").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Remove reminder").assertDoesNotExist()
        onNodeWithText("+ Reminder").assertDoesNotExist()
    }

    @Test
    fun backRemainsAvailableWhenTheItemCannotBeFound() = runComposeUiTest {
        var backs = 0
        val viewModel = model("missing", items)
        setContent { OrbitTheme { ItemScreen("space-1", "missing", { backs++ }, viewModel) } }
        onNodeWithContentDescription("Back to space").performClick()
        assertEquals(1, backs)
    }

    private fun createItem(item: NewItem): String = runBlocking { items.create("space-1", item) }

    @Test
    fun editsTheScheduleFromItsCard() = runComposeUiTest {
        val itemId = runBlocking {
            items.create("space-1", NewItem("Dinner", modules = ItemModules(schedule = Schedule(LocalDate(2026, 10, 26)))))
        }
        val viewModel = model(itemId, items)
        setContent {
            OrbitTheme {
                ItemScreen(spaceId = "space-1", itemId = itemId, onBack = {}, viewModel = viewModel)
            }
        }

        onNodeWithContentDescription("Edit schedule").performScrollTo().performClick()
        onNodeWithText("On Oct 26, 2026").assertIsDisplayed()
        onNodeWithText("Time (HH:MM), optional").performTextInput("25:00")
        onNodeWithText("Use a time like 09:30.").assertIsDisplayed()
        onNodeWithText("Save").performScrollTo().assertIsNotEnabled()

        onNodeWithText("25:00").performTextReplacement("20:00")
        onNodeWithText("Save").performScrollTo().assertIsEnabled().performClick()
        waitForIdle()

        val saved = runBlocking { items.observeItems("space-1").first().single { it.id == itemId } }
        assertEquals(Schedule(LocalDate(2026, 10, 26), time = LocalTime(20, 0)), saved.modules.schedule)
        onNodeWithText("Save").assertDoesNotExist()
    }

    private fun model(id: String, repository: ItemRepository): ItemViewModel =
        ItemViewModel("space-1", id, repository, spaces).also { viewModels.put("item", it) }
}
