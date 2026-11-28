package com.orbit.app.e2e

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.App
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.deviceMoneyLocale
import com.orbit.app.core.moneyInput
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.SpaceProposal
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryExpenseRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemoryPlanRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.testing.ScriptedAssistantRepository
import com.orbit.app.testing.startTestKoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The main flows through the whole app, from its first screen and its real navigation, on the test fakes (#123).
 * Each screen has its own tests: these check that the screens join up into what a user does. What fakes cannot show
 * (sync between phones, offline, push) is the two-device checklist in docs/process/testing.md.
 */
@OptIn(ExperimentalTestApi::class)
class MainFlowsTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)
    private val expenses = InMemoryExpenseRepository(auth)

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private fun ComposeUiTest.launch(assistant: ScriptedAssistantRepository = ScriptedAssistantRepository(emptyList())) {
        startTestKoin(
            auth,
            spaces,
            items = items,
            expenses = expenses,
            assistant = assistant,
            plans = InMemoryPlanRepository(spaces, items),
        )
        setContent { App() }
    }

    /** Ana is signed in and owns Flat 4B, which Ben shares, so the app opens on the list of spaces. */
    private fun anasFlat() = runBlocking {
        auth.register("ana@orbit.app", "secret", "Ana")
        spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember("space-1", Member("ben@orbit.app", "Ben", photoUrl = null, role = Role.MEMBER))
    }

    private fun ComposeUiTest.openFlat() {
        onNodeWithText("Flat 4B").performScrollTo().performClick()
    }

    @Test
    fun signsInAndCreatesASpace() = runComposeUiTest {
        launch()

        onNodeWithText("Email address").performTextInput("ana@orbit.app")
        onNodeWithText("Password").performTextInput("secret")
        onNodeWithText("Sign in").performScrollTo().performClick()

        onNodeWithText("No spaces yet").assertIsDisplayed()
        onNodeWithContentDescription("New space").performClick()
        onAllNodes(hasSetTextAction())[0].performTextInput("Flat 4B")
        onNodeWithText("Create space").performScrollTo().performClick()

        // The new space opens on its home, with its name in the top bar.
        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("Calendar").assertIsDisplayed()
    }

    @Test
    fun addsAnItemAndCompletesIt() = runComposeUiTest {
        anasFlat()
        launch()
        openFlat()

        onAllNodesWithText("Tasks").onFirst().performClick()
        onNodeWithContentDescription("Add").performClick()
        onNodeWithText("New task").performClick()
        onNodeWithContentDescription("Name").performTextInput("Take the bins out")
        onNodeWithText("Create").performClick()

        onNodeWithText("Take the bins out").performScrollTo().performClick()
        onNodeWithText("Mark as done").performClick()
        waitForIdle()

        val item = runBlocking { items.observeItems("space-1").first() }.single()
        assertEquals("Take the bins out", item.title)
        assertEquals("ana@orbit.app", (item.status as ItemStatus.Done).by)
    }

    @Test
    fun addsAnExpenseAndSettlesUp() = runComposeUiTest {
        anasFlat()
        launch()
        openFlat()

        onNodeWithText("Expenses").performClick()
        onNodeWithContentDescription("Add expense").performClick()
        onNode(hasSetTextAction() and hasContentDescription("Title")).performTextInput("Groceries")
        onNode(hasSetTextAction() and hasContentDescription("Amount")).performScrollTo()
            .performTextInput(moneyInput(4000, deviceMoneyLocale()))
        onNode(hasText("Paid by") and hasText("Ana")).performScrollTo().performClick()
        onAllNodesWithText("Ben").onLast().performClick()
        onNodeWithText("Save").performClick()

        // Ben paid and the cost is split evenly, so Ana owes him half, and she is the one who says she paid it.
        onNodeWithText("Groceries").performScrollTo().assertIsDisplayed()
        onNodeWithText("You owe Ben").performScrollTo().assertIsDisplayed()
        onNodeWithContentDescription("Mark as paid: You owe Ben").performScrollTo().performClick()
        onAllNodesWithText("Mark as paid").onLast().performScrollTo().performClick()

        onNode(hasText("Ana") and hasText("You're settled up")).performScrollTo().assertIsDisplayed()
        onNode(hasText("Ben") and hasText("Settled up")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun reviewsAndConfirmsAnAssistantPlan() = runComposeUiTest {
        anasFlat()
        runBlocking { auth.acceptAiConsent() }
        val who = InterviewQuestion("q1", "Who lives there?", listOf("Roommates", "Family"), false, true)
        val plan = Plan(space = null, items = listOf(NewItem("Bins"), NewItem("Groceries")))
        launch(ScriptedAssistantRepository(listOf(InterviewStep.Ask(who), InterviewStep.PlanReady(plan))))
        openFlat()

        onAllNodesWithText("Tasks").onFirst().performClick()
        onNodeWithContentDescription("Add").performClick()
        onNodeWithText("Plan with the assistant").performClick()
        onNodeWithText("Roommates").performClick()
        onNodeWithContentDescription("Send").performClick()

        // Nothing is saved until the plan is confirmed.
        onNodeWithText("Accept all").performScrollTo().performClick()
        assertEquals(emptyList(), runBlocking { items.observeItems("space-1").first() })
        onNodeWithText("Continue").performClick()
        onNodeWithText("Confirm").performScrollTo().performClick()
        waitForIdle()

        val saved = runBlocking { items.observeItems("space-1").first() }.map { it.title }.toSet()
        assertEquals(setOf("Bins", "Groceries"), saved)
        onNodeWithText("Bins").performScrollTo().assertIsDisplayed()
    }
}
