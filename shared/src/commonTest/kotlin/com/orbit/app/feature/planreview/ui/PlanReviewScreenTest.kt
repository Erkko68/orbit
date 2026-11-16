package com.orbit.app.feature.planreview.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.PlanDraft
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.SpaceProposal
import com.orbit.app.feature.planreview.PlanReviewViewModel
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PlanReviewScreenTest : ComposeTest() {
    private val auth = InMemoryAuthRepository()
    private val spaces = FakeSpaceRepository.withMembers("ana" to Role.OWNER)

    private fun draft(space: SpaceProposal? = null, items: List<NewItem> = listOf(NewItem("Bins"), NewItem("Groceries"))) =
        PlanDraft().apply { propose(Plan(space, items)) }

    @Test
    fun showsTheProposalsUnderTheSpaceWithTheCount() = runComposeUiTest {
        val viewModel = PlanReviewViewModel("space-1", draft(), spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, {}, viewModel) } }

        onNodeWithText("Flat 4B").assertIsDisplayed()
        onNodeWithText("Proposed").assertIsDisplayed()
        onNodeWithText("0 of 2 accepted").assertIsDisplayed()
        onNodeWithText("Bins").assertIsDisplayed()
        onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun theCheckAcceptsAndAcceptAllAcceptsTheRest() = runComposeUiTest {
        val plan = draft()
        val viewModel = PlanReviewViewModel("space-1", plan, spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, {}, viewModel) } }

        onNodeWithContentDescription("Accept Bins").performClick()
        onNodeWithText("1 of 2 accepted").assertIsDisplayed()

        onNodeWithText("Accept all").performClick()
        onNodeWithText("2 of 2 accepted").assertIsDisplayed()
        onNodeWithText("Accept all").assertIsNotEnabled()
        assertEquals(2, plan.review.value?.accepted?.size)
    }

    @Test
    fun aTapOpensTheProposal() = runComposeUiTest {
        var opened: String? = null
        val viewModel = PlanReviewViewModel("space-1", draft(), spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", {}, { opened = it }, viewModel) } }

        onNodeWithText("Groceries").performClick()

        assertEquals("proposal-1", opened)
    }

    @Test
    fun aNewSpaceShowsItsProposedName() = runComposeUiTest {
        val viewModel = PlanReviewViewModel("", draft(SpaceProposal("New flat", accent = 0)), spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("", {}, {}, viewModel) } }

        onNodeWithText("New flat").assertIsDisplayed()
    }

    @Test
    fun anEmptyPlanSaysThereIsNothingToReview() = runComposeUiTest {
        var backs = 0
        val viewModel = PlanReviewViewModel("space-1", PlanDraft(), spaces, auth)
        setContent { OrbitTheme { PlanReviewScreen("space-1", { backs++ }, {}, viewModel) } }

        onNodeWithText("Nothing to review").assertIsDisplayed()
        onNodeWithText("Accept all").assertDoesNotExist()
        onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backs)
    }
}
