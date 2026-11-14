package com.orbit.app.feature.item.ui

import com.orbit.app.testing.InMemoryExpenseRepository
import com.orbit.app.feature.expense.ExpenseViewModel
import com.orbit.app.feature.expense.ui.ExpenseScreen
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
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
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.feature.item.ItemViewModel
import com.orbit.app.testing.ControlledItemRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.startTestKoin
import com.orbit.app.domain.model.Cost
import com.orbit.app.core.formatEuro
import com.orbit.app.core.navigation.encodePlannedCost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalTestApi::class)
class ItemScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository().apply {
        runBlocking { signIn("ana", "secret") }
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

    @BeforeTest
    fun setUp() {
        startTestKoin(auth, spaces, items)
    }

    @AfterTest
    fun tearDown() {
        viewModels.clear()
        stopKoin()
    }

    @Test
    fun aChecklistOpensTheExpenseFormAndBackDiscardsThePurchase() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Weekly shopping", modules = ItemModules(
                    checklist = Checklist(listOf(ChecklistEntry("milk", "Milk"))),
                )
            )
        )
        val itemModel = model(id, items)
        val ledger = InMemoryExpenseRepository(auth)
        val expenseModel = ExpenseViewModel(
            "space-1", "", "", "", ledger, spaces, auth, items, sourceItemId = id,
        )
        var expenseOpen by mutableStateOf(false)
        setContent {
            OrbitTheme {
                if (expenseOpen) {
                    ExpenseScreen(
                        "space-1", "", { expenseOpen = false }, sourceItemId = id, viewModel = expenseModel,
                    )
                } else {
                    ItemScreen("space-1", id, {}, itemModel, onAddExpense = { expenseOpen = true })
                }
            }
        }
        list().performScrollToNode(hasText("Add as expense"))
        onNodeWithText("Add as expense").performClick()
        onNodeWithText("Save").assertIsNotEnabled()
        onNode(hasSetTextAction() and hasContentDescription("Amount")).assertIsFocused()
        onNode(hasSetTextAction() and hasContentDescription("Title")).assertIsDisplayed()
        onNodeWithText("Date").performScrollTo().assertIsDisplayed()
        onNode(hasSetTextAction() and hasContentDescription("Amount")).performScrollTo().performTextInput("10")
        onNodeWithContentDescription("Back").performClick()
        list().performScrollToNode(hasText("Milk"))
        onNodeWithText("Milk").assertIsDisplayed()
        assertTrue(runBlocking { ledger.observeExpenses("space-1").first().isEmpty() })
        assertFalse(runBlocking {
            items.observeItems("space-1").first().single().modules.checklist!!.entries.single().checked
        })
    }

    @Test
    fun aNewItemPlansItsCostAndCreateWritesItWithTheItem() = runComposeUiTest {
        val opened = mutableListOf<String>()
        val viewModel = model("", items)
        val cost = Cost("ana", mapOf("ana" to 600L, "mika" to 400L))
        var planned by mutableStateOf("")
        setContent {
            OrbitTheme {
                ItemScreen(
                    "space-1", "", {}, viewModel,
                    onOpenCost = { opened += it },
                    plannedCost = planned,
                    onPlannedCostTaken = { planned = "" },
                )
            }
        }

        list().performScrollToNode(hasText("Add a cost"))
        onNodeWithText("Cost").performClick()
        planned = encodePlannedCost(cost)
        list().performScrollToNode(hasText(formatEuro(1000)))
        onNodeWithText("Cost").performClick()
        onNodeWithContentDescription("Name").performTextInput("Rent")
        onNodeWithText("Create").performClick()
        waitForIdle()

        assertEquals(listOf(encodePlannedCost(null), encodePlannedCost(cost)), opened)
        assertEquals(cost, runBlocking { items.observeItems("space-1").first().single().modules.cost })
    }

    @Test
    fun theCostRowOfAWrittenItemOpensItsCost() = runComposeUiTest {
        var opened = 0
        val id = createItem(NewItem("Detergent"))
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, model(id, items), onOpenCost = { opened++ }) } }

        list().performScrollToNode(hasText("Add a cost"))
        onNodeWithText("Cost").performClick()
        runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun aCostChargingSomeoneWhoLeftSaysSoAndBlocksMarkDone() = runComposeUiTest {
        val id = createItem(NewItem("Rent", modules = ItemModules(cost = Cost("ana", mapOf("zoe" to 1000L)))))
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, model(id, items)) } }

        onNodeWithText("Someone in the cost has left the space. Change the cost before marking the item done.").assertExists()
        onNodeWithText("Mark as done").assertIsNotEnabled()
    }

    @Test
    fun listsWhoCompletedTheItemUnderIt() = runComposeUiTest {
        val id = createItem(NewItem("Take the bins out"))
        runBlocking {
            items.complete("space-1", id, null)
            items.reopen("space-1", id)
        }
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        list().performScrollToNode(hasText("History"))
        onNodeWithText("History").assertIsDisplayed()
        list().performScrollToNode(hasText("You created it"))
        onNodeWithText("You marked it done").assertExists()
        onNodeWithText("You reopened it").assertExists()
        onNodeWithText("You created it").assertIsDisplayed()
    }

    @Test
    fun aNewItemHasNoHistory() = runComposeUiTest {
        setContent { OrbitTheme { ItemScreen("space-1", "", {}, model("", items)) } }

        onNodeWithText("History").assertDoesNotExist()
    }

    @Test
    fun showsTheHeaderTheDateAndTheChecklist() = runComposeUiTest {
        val schedule = Schedule(date = LocalDate(2026, 10, 26))
        val checklist = Checklist(listOf(ChecklistEntry("entry-1", "Kitchen faucet", checked = false)))
        val id = createItem(
            NewItem("Book plumber", "Kitchen leak", ItemModules(schedule = schedule, checklist = checklist)),
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        val label = onNodeWithText("Flat 4B").fetchSemanticsNode()
        val title = onNodeWithText("Book plumber").fetchSemanticsNode()
        assertTrue(label.boundsInRoot.bottom <= title.boundsInRoot.top)
        onNodeWithText("Oct 26, 2026").assertIsDisplayed()
        onNodeWithText("Remind me").assertIsDisplayed()
        list().performScrollToNode(hasText("Kitchen leak"))
        onNodeWithText("Kitchen leak").assertIsDisplayed()
        list().performScrollToNode(hasText("Kitchen faucet"))
        onNodeWithText("Kitchen faucet").assertIsDisplayed()
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
        val id = createItem(NewItem("Recoverable task"))
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
    fun aPendingWriteDisablesTheActions() = runComposeUiTest {
        val id = createItem(NewItem("Pending task"))
        val gate = CompletableDeferred<Unit>()
        val repository = ControlledItemRepository(items).apply { beforeWrite = { gate.await() } }
        val viewModel = model(id, repository)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithText("Mark as done").performClick()
        onNodeWithText("Mark as done").assertIsNotEnabled()
        runOnIdle { gate.complete(Unit) }
        onNodeWithText("Reopen").assertIsEnabled()
        assertEquals(listOf("complete"), repository.writes)
    }

    @Test
    fun aFailedWriteShowsAnErrorAndTheNextAttemptCanSucceed() = runComposeUiTest {
        val id = createItem(NewItem("Task"))
        val repository = ControlledItemRepository(items).apply { beforeWrite = { error("Write denied") } }
        val viewModel = model(id, repository)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithText("Mark as done").performClick()
        onNodeWithText("Action failed. Try again.").assertIsDisplayed()

        repository.beforeWrite = {}
        onNodeWithText("Mark as done").performClick()
        waitForIdle()

        onNodeWithText("Action failed. Try again.").assertDoesNotExist()
        onNodeWithText("Reopen").assertIsDisplayed()
    }

    @Test
    fun aChecklistIsOneListWithWhatIsLeftFirst() = runComposeUiTest {
        val entries = listOf(ChecklistEntry("milk", "Milk", true, 2), ChecklistEntry("bread", "Bread", false))
        val id = createItem(NewItem("Groceries", modules = ItemModules(checklist = Checklist(entries))))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        list().performScrollToNode(hasText("Milk"))
        val bread = onNodeWithText("Bread").fetchSemanticsNode()
        val milk = onNodeWithText("Milk").fetchSemanticsNode()
        assertTrue(bread.boundsInRoot.bottom <= milk.boundsInRoot.top)
        onNodeWithText("× 2", useUnmergedTree = true).assertExists()
        onNodeWithText("1 of 2", useUnmergedTree = true).assertExists()
        onNodeWithText("Done").assertDoesNotExist()
        onNodeWithText("Mark as done").assertDoesNotExist()
    }

    @Test
    fun anEntryIsAddedTickedEditedAndRemoved() = runComposeUiTest {
        val id = createItem(NewItem("Groceries", modules = ItemModules(checklist = Checklist(emptyList()))))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        list().performScrollToNode(hasContentDescription("Add to the list"))
        addField().performTextInput("Milk")
        // The keyboard's Done adds it: the button under the bottom bar would take the tap.
        addField().performImeAction()
        waitForIdle()

        list().performScrollToNode(hasText("Milk"))
        onNodeWithContentDescription("Check Milk").performClick()
        onNodeWithContentDescription("Uncheck Milk").assertExists()
        onNodeWithText("Milk").performClick()
        onNodeWithText("Remove item").performScrollTo().performClick()
        waitForIdle()

        onNodeWithText("Milk").assertDoesNotExist()
    }

    @Test
    fun eventRendersScheduleDurationAndReminderOffsets() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Dinner", modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 10, 26), time = LocalTime(20, 0), duration = 30.minutes),
                    reminder = Reminder(listOf(Duration.ZERO, 10.minutes)),
                    assignment = Assignment.Anyone,
                )
            )
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Oct 26, 2026 · 20:00").assertIsDisplayed()
        onNodeWithContentDescription("Ana").assertExists()
        onNodeWithContentDescription("Mika").assertExists()
        onNodeWithText("2 reminders").assertIsDisplayed()
    }

    @Test
    fun theHeaderSaysWhichSpaceItIsIn() = runComposeUiTest {
        val id = createItem(NewItem("Dinner"))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("Dinner").assertIsDisplayed()
    }

    @Test
    fun aDateRangeAndAnyonesItemAreShown() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Window", modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 10, 26), endDate = LocalDate(2026, 10, 28)),
                    assignment = Assignment.Anyone,
                    reminder = Reminder(emptyList()),
                )
            )
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Oct 26, 2026 – Oct 28, 2026").assertIsDisplayed()
        onNodeWithContentDescription("Ana").assertExists()
        onNodeWithText("Remind me").assertIsDisplayed()
    }

    @Test
    fun memberAndClaimAssignmentsDisplayTheStoredAssignee() = runComposeUiTest {
        val id =
            createItem(NewItem("Assigned task", modules = ItemModules(assignment = Assignment.ToMembers(setOf("ana")))))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithContentDescription("Ana").assertExists()
        runOnIdle { runBlocking { items.setModule("space-1", id, Assignment.ToMembers(setOf("mika"))) } }
        onNodeWithContentDescription("Mika").assertExists()
        onNodeWithContentDescription("Ana").assertDoesNotExist()
    }

    @Test
    fun darkThemeKeepsMissingMemberAndScheduleReadable() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Shared task",
                modules = ItemModules(
                    assignment = Assignment.ToMembers(setOf("missing")),
                    schedule = Schedule(LocalDate(2026, 10, 26), time = LocalTime(20, 0)),
                ),
            ),
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme(darkTheme = true) { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithText("Oct 26, 2026 · 20:00").assertIsDisplayed()
        onNodeWithText("Add who does it").assertIsDisplayed()
    }

    @Test
    fun removingTheDateRemovesTheReminder() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Reminder task", modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 10, 26)), reminder = Reminder(listOf(Duration.ZERO)),
                )
            )
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }
        onNodeWithText("Oct 26, 2026").performClick()
        onNodeWithText("Remove the date").performScrollTo().performClick()
        waitForIdle()
        onNodeWithText("At the due time").assertDoesNotExist()
        onNodeWithText("Remind me").assertDoesNotExist()
        onNodeWithText("Add a date").assertIsDisplayed()
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
    fun aNewItemIsCreatedOnceNamedAndTheScreenCloses() = runComposeUiTest {
        var backs = 0
        val viewModel = model("", items)
        setContent {
            OrbitTheme {
                ItemScreen(
                    spaceId = "space-1",
                    itemId = "",
                    onBack = { backs++ },
                    viewModel = viewModel
                )
            }
        }

        onNodeWithText("Create").assertIsNotEnabled()
        onNodeWithText("Mark as done").assertDoesNotExist()
        onNodeWithContentDescription("Delete").assertDoesNotExist()

        onNodeWithContentDescription("Name").performTextInput("Buy milk")
        onNodeWithText("Create").assertIsEnabled().performClick()
        waitForIdle()

        assertEquals(1, backs)
        assertEquals("Buy milk", runBlocking { items.observeItems("space-1").first() }.single().title)
    }

    @Test
    fun editsTheScheduleFromItsRow() = runComposeUiTest {
        val itemId = runBlocking {
            items.create(
                "space-1",
                NewItem("Dinner", modules = ItemModules(schedule = Schedule(LocalDate(2026, 10, 26))))
            )
        }
        val viewModel = model(itemId, items)
        setContent {
            OrbitTheme {
                ItemScreen(spaceId = "space-1", itemId = itemId, onBack = {}, viewModel = viewModel)
            }
        }

        onNodeWithText("Oct 26, 2026").performClick()
        onNodeWithText("On Oct 26, 2026").assertIsDisplayed()
        onNodeWithText("Add a time").performClick()
        onNodeWithText("Pick a time").assertIsDisplayed()
        onNodeWithText("Choose").performScrollTo().performClick()
        onNodeWithText("09:00").assertIsDisplayed()
        onNodeWithContentDescription("Min").performScrollTo().performTextInput("30")

        onNodeWithText("Save").performScrollTo().assertIsEnabled().performClick()
        waitForIdle()

        val saved = runBlocking { items.observeItems("space-1").first().single { it.id == itemId } }
        assertEquals(
            Schedule(LocalDate(2026, 10, 26), time = LocalTime(9, 0), duration = 30.minutes),
            saved.modules.schedule,
        )
        onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun repeatsTheItemFromTheWhenSheetAndShowsItInTheRow() = runComposeUiTest {
        val itemId = createItem(NewItem("Bins", modules = ItemModules(schedule = Schedule(LocalDate(2030, 1, 7)))))
        val viewModel = model(itemId, items)
        setContent { OrbitTheme { ItemScreen("space-1", itemId, {}, viewModel) } }

        onNodeWithText("Jan 7, 2030").performClick()
        onNodeWithText("Repeat").performScrollTo().performClick()
        onNodeWithText("Every week · Mon").assertIsDisplayed()
        onNodeWithText("Every year").assertIsDisplayed()
        onNodeWithText("Custom").performScrollTo().performClick()
        onNodeWithText("Every").assertIsDisplayed()
        onNodeWithText("Fri").performScrollTo().performClick()
        onNodeWithText("Ends").performScrollTo().assertIsDisplayed()
        onNodeWithText("Done").performScrollTo().performClick()
        onNodeWithText("Every week · Mon, Fri").performScrollTo().assertIsDisplayed()
        onNodeWithText("Save").performScrollTo().performClick()
        waitForIdle()

        val saved = runBlocking { items.observeItems("space-1").first().single { it.id == itemId } }
        assertEquals(Recurrence.Weeks(1, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)), saved.modules.recurrence)
        onNodeWithText("Jan 7, 2030 · Every week · Mon, Fri").assertIsDisplayed()
    }

    @Test
    fun marksTheItemDoneAndReopensItFromTheBottomButton() = runComposeUiTest {
        val itemId = createItem(NewItem("Buy milk"))
        val viewModel = model(itemId, items)
        setContent { OrbitTheme { ItemScreen("space-1", itemId, {}, viewModel) } }

        onNodeWithText("Mark as done").performClick()
        waitForIdle()

        onNodeWithText("Reopen").assertIsDisplayed()
        val completed = runBlocking { items.observeItems("space-1").first().single { it.id == itemId } }
        assertTrue(completed.status is ItemStatus.Done)

        onNodeWithText("Reopen").performClick()
        waitForIdle()

        onNodeWithText("Mark as done").assertIsDisplayed()
        val reopened = runBlocking { items.observeItems("space-1").first().single { it.id == itemId } }
        assertTrue(reopened.status is ItemStatus.Open)
    }

    @Test
    fun theWhoRowGivesTheItemToAMemberAndShowsTheirFace() = runComposeUiTest {
        val id = createItem(NewItem("Task"))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithText("Add who does it").performClick()
        waitForIdle()
        onNodeWithText("Mika").performClick()
        waitForIdle()
        onNodeWithContentDescription("Close").performClick()
        waitForIdle()

        onNodeWithContentDescription("Mika").assertExists()
        val updated = runBlocking { items.observeItems("space-1").first().single { it.id == id } }
        assertEquals(Assignment.ToMembers(setOf("mika")), updated.modules.assignment)
    }

    @Test
    fun theSheetMakesAnItemAnyones() = runComposeUiTest {
        val id = createItem(NewItem("Task", modules = ItemModules(assignment = Assignment.ToMembers(setOf("ana")))))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        runOnIdle { viewModel.openMemberPicker() }
        onNodeWithText("Anyone").performScrollTo().performClick()
        waitForIdle()

        val updated = runBlocking { items.observeItems("space-1").first().single { it.id == id } }
        assertEquals(Assignment.Anyone, updated.modules.assignment)
    }

    @Test
    fun aDateIsAddedFromItsRow() = runComposeUiTest {
        val id = createItem(NewItem("Task"))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithText("Add a date").performClick()
        onNodeWithText("Save").performScrollTo().performClick()
        waitForIdle()

        val updated = runBlocking { items.observeItems("space-1").first().single { it.id == id } }
        assertTrue(updated.modules.schedule != null)
    }

    @Test
    fun theReminderRowShowsOnceTheItemHasADate() = runComposeUiTest {
        val id = createItem(NewItem("Task"))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithText("Remind me").assertDoesNotExist()
        onNodeWithText("Add a date").performClick()
        onNodeWithText("Save").performScrollTo().performClick()
        waitForIdle()

        onNodeWithText("Remind me").assertIsDisplayed()
    }

    @Test
    fun theDescriptionIsTypedWhereItIsRead() = runComposeUiTest {
        val id = createItem(NewItem("Task"))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        list().performScrollToNode(hasContentDescription("Add a description"))
        onNodeWithContentDescription("Add a description").performTextInput("Sink leaking")
        runOnIdle { viewModel.saveDrafts() }
        waitForIdle()

        val updated = runBlocking { items.observeItems("space-1").first().single { it.id == id } }
        assertEquals("Sink leaking", updated.description)
    }

    @Test
    fun theNameIsTypedInTheHeader() = runComposeUiTest {
        val id = createItem(NewItem("Task"))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithContentDescription("Name").performTextReplacement("Call the plumber")
        onNodeWithContentDescription("Name").performImeAction()
        waitForIdle()

        val updated = runBlocking { items.observeItems("space-1").first().single { it.id == id } }
        assertEquals("Call the plumber", updated.title)
    }

    @Test
    fun typingInTheAddRowKeepsEveryLetter() = runComposeUiTest {
        val id = createItem(NewItem("Groceries", modules = ItemModules(checklist = Checklist(emptyList()))))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        list().performScrollToNode(hasContentDescription("Add to the list"))
        addField().performTextInput("m")
        addField().performTextInput("i")
        addField().performTextInput("lk")
        waitForIdle()

        assertEquals("milk", viewModel.uiState.value.newEntry)
    }

    @Test
    fun aReminderIsChangedFromItsRow() = runComposeUiTest {
        val id = createItem(
            NewItem(
                "Task",
                modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 10, 26)),
                    reminder = Reminder(listOf(Duration.ZERO))
                ),
            ),
        )
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithText("At the due time").performClick()
        onNodeWithText("Add reminder").performScrollTo().performClick()
        onNodeWithText("10 min before").performScrollTo().performClick()
        waitForIdle()

        val reminded = runBlocking { items.observeItems("space-1").first().single { it.id == id } }
        assertEquals(Reminder(listOf(Duration.ZERO, 10.minutes)), reminded.modules.reminder)
        onNodeWithText("10 min before").assertIsDisplayed()

        onNodeWithContentDescription("Remove At the due time").performClick()
        waitForIdle()
        val removed = runBlocking { items.observeItems("space-1").first().single { it.id == id } }
        assertEquals(Reminder(listOf(10.minutes)), removed.modules.reminder)
    }

    @Test
    fun theDeleteButtonIsOnlyForWhoCanDelete() = runComposeUiTest {
        val id = runBlocking {
            auth.signIn("mika", "secret")
            items.create("space-1", NewItem("Task")).also { auth.signIn("ana", "secret") }
        }
        val member = FakeSpaceRepository(
            com.orbit.app.domain.model.Space("space-1", "Flat 4B", 0, listOf("ana", "mika")),
            listOf(com.orbit.app.domain.model.Member("ana", "Ana", null, Role.MEMBER)),
        )
        val viewModel = ItemViewModel("space-1", id, items, member, auth).also { viewModels.put("item", it) }
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    @Test
    fun deletingAsksFirstAndLeavesTheScreen() = runComposeUiTest {
        var backs = 0
        val id = createItem(NewItem("Task"))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, { backs++ }, viewModel) } }

        onNodeWithContentDescription("Delete").performClick()
        onNodeWithText("Delete this?").assertIsDisplayed()
        onNodeWithText("Delete").performClick()
        waitForIdle()

        assertEquals(1, backs)
        assertTrue(runBlocking { items.observeItems("space-1").first() }.none { it.id == id })
    }

    private fun model(id: String, repository: ItemRepository): ItemViewModel =
        ItemViewModel("space-1", id, repository, spaces, auth).also { viewModels.put("item", it) }

    @Test
    fun tickingTheLastEntryFinishesTheItem() = runComposeUiTest {
        val entries = listOf(ChecklistEntry("milk", "Milk", checked = true), ChecklistEntry("bread", "Bread"))
        val id = createItem(NewItem("Groceries", modules = ItemModules(checklist = Checklist(entries))))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        list().performScrollToNode(hasContentDescription("Check Bread"))
        onNodeWithContentDescription("Check Bread").performClick()
        waitForIdle()

        val finished = runBlocking { items.observeItems("space-1").first().single { it.id == id } }
        assertTrue(finished.status is ItemStatus.Done)
        onNodeWithText("Reopen").assertDoesNotExist()
    }

    @Test
    fun anyItemHasAChecklistToAddTo() = runComposeUiTest {
        val id = createItem(NewItem("Groceries"))
        val viewModel = model(id, items)
        setContent { OrbitTheme { ItemScreen("space-1", id, {}, viewModel) } }

        list().performScrollToNode(hasContentDescription("Add to the list"))
        addField().performTextInput("Eggs")
        addButton().performClick()
        waitForIdle()

        list().performScrollToNode(hasText("Eggs"))
        onNodeWithText("Eggs").assertIsDisplayed()
    }

    private fun ComposeUiTest.list() = onNode(hasScrollAction())

    private fun ComposeUiTest.addField() = onNode(hasSetTextAction() and hasContentDescription("Add to the list"))

    private fun ComposeUiTest.addButton() =
        onNode(hasContentDescription("Add to the list") and hasClickAction() and !hasSetTextAction())
}
