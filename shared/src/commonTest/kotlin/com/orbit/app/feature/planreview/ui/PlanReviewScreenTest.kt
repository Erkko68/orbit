package com.orbit.app.feature.planreview.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.PlanDraft
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.PlanReview
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.SpaceProposal
import com.orbit.app.domain.repository.PlanRepository
import com.orbit.app.feature.planreview.PlanReviewViewModel
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemoryPlanRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PlanReviewScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER)
    private val items = InMemoryItemRepository(auth)
    private val plans = InMemoryPlanRepository(InMemorySpaceRepository(auth), items)

    private fun draft(space: SpaceProposal? = null, items: List<NewItem> = listOf(NewItem("Bins"), NewItem("Groceries"))) =
        PlanDraft().apply { propose(Plan(space, items)) }

    @Test
    fun showsTheProposalsUnderTheSpaceWithTheCount() = runComposeUiTest {
        val viewModel = PlanReviewViewModel("space-1", draft(), plans, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, {}, {}, viewModel) } }

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("Proposed").assertIsDisplayed()
        onNodeWithText("0 of 2 accepted").assertIsDisplayed()
        onNodeWithText("Bins").assertIsDisplayed()
        onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun theCheckAcceptsAndAcceptAllAcceptsTheRest() = runComposeUiTest {
        val plan = draft()
        val viewModel = PlanReviewViewModel("space-1", plan, plans, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, {}, {}, viewModel) } }

        onNodeWithContentDescription("Accept Bins").performClick()
        onNodeWithText("1 of 2 accepted").assertIsDisplayed()

        onNodeWithText("Accept all").performScrollTo().performClick()
        onNodeWithText("2 of 2 accepted").assertIsDisplayed()
        onNodeWithText("Accept all").assertDoesNotExist()
        assertEquals(2, plan.review.value?.accepted?.size)
    }

    @Test
    fun aTapOpensTheProposal() = runComposeUiTest {
        var opened: String? = null
        val viewModel = PlanReviewViewModel("space-1", draft(), plans, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, { opened = it }, {}, viewModel) } }

        onNodeWithText("Groceries").performClick()

        assertEquals("proposal-1", opened)
    }

    @Test
    fun aNewSpaceShowsItsProposedName() = runComposeUiTest {
        val viewModel = PlanReviewViewModel("", draft(SpaceProposal("New flat", accent = 0)), plans, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("", {}, {}, {}, viewModel) } }

        onNodeWithText("New flat").assertIsDisplayed()
    }

    @Test
    fun anEmptyPlanSaysThereIsNothingToReview() = runComposeUiTest {
        var backs = 0
        val viewModel = PlanReviewViewModel("space-1", PlanDraft(), plans, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", { backs++ }, {}, {}, viewModel) } }

        onNodeWithText("Nothing to review").assertIsDisplayed()
        onNodeWithText("Accept all").assertDoesNotExist()
        onNodeWithText("Continue").assertDoesNotExist()
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun continueSumsUpAndConfirmSavesAndOpensTheSpace() = runComposeUiTest {
        var saved: String? = null
        runBlocking { auth.signIn("ana@orbit.app", "secret") }
        val viewModel = PlanReviewViewModel("space-1", draft(), plans, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, {}, { saved = it }, viewModel) } }

        onNodeWithText("Continue").assertIsNotEnabled()
        onNodeWithContentDescription("Accept Bins").performClick()
        onNodeWithText("Continue").assertIsEnabled().performClick()
        onNodeWithText("Adds 1 item to Flat 4B.").assertIsDisplayed()
        onNodeWithText("Confirm").performScrollTo().performClick()

        waitForIdle()
        assertEquals("space-1", saved)
        assertEquals(listOf("Bins"), runBlocking { items.observeItems("space-1").first() }.map { it.title })
    }

    @Test
    fun continueShowsWhatTheAssistantWillRemember() = runComposeUiTest {
        val plan = PlanDraft().apply { propose(Plan(null, listOf(NewItem("Bins")), summary = "Three flatmates who take turns.")) }
        val viewModel = PlanReviewViewModel("space-1", plan, plans, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, {}, {}, viewModel) } }

        onNodeWithText("Accept all").performScrollTo().performClick()
        onNodeWithText("Continue").performClick()

        onNodeWithText("What the assistant will remember about this space").assertIsDisplayed()
        onNodeWithText("Three flatmates who take turns.").assertIsDisplayed()
    }

    @Test
    fun aNewSpaceSaysItWillBeCreated() = runComposeUiTest {
        val viewModel = PlanReviewViewModel("", draft(SpaceProposal("New flat", accent = 0)), plans, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("", {}, {}, {}, viewModel) } }

        onNodeWithText("Accept all").performScrollTo().performClick()
        onNodeWithText("Continue").performClick()

        onNodeWithText("Creates the space New flat with 2 items. You can invite people from its settings.")
            .assertIsDisplayed()
    }

    @Test
    fun aFailedSaveSaysNothingWasAdded() = runComposeUiTest {
        val failing = object : PlanRepository {
            override suspend fun confirm(spaceId: String, plan: PlanReview): String = error("Refused")
        }
        val viewModel = PlanReviewViewModel("space-1", draft(), failing, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, {}, {}, viewModel) } }

        onNodeWithText("Accept all").performScrollTo().performClick()
        onNodeWithText("Continue").performClick()
        onNodeWithText("Confirm").performScrollTo().performClick()

        onNodeWithText("The plan could not be saved, so nothing was added. Try again.").assertIsDisplayed()
    }
}
