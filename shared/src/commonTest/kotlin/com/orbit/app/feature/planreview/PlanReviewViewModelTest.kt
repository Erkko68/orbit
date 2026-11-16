package com.orbit.app.feature.planreview

import com.orbit.app.core.PlanDraft
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.SpaceProposal
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlanReviewViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val bins = NewItem("Bins", modules = ItemModules(assignment = Assignment.ToMembers(setOf("ben"))))
    private val groceries = NewItem("Groceries", description = "Milk\nBread")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.board(
        plan: PlanDraft,
        spaceId: String = "space-1",
        spaces: SpaceRepository = FakeSpaceRepository.withMembers("ana" to Role.OWNER, "ben" to Role.MEMBER),
    ) = PlanReviewViewModel(spaceId, plan, spaces, auth).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun draft(space: SpaceProposal? = null) = PlanDraft().apply { propose(Plan(space, listOf(bins, groceries))) }

    @Test
    fun showsEachProposalAsItsTasksRowInTheSpace() = runTest {
        val state = board(draft()).uiState.value

        assertFalse(state.missing)
        assertEquals("Flat 4B", state.spaceName)
        assertEquals(listOf("Bins", "Groceries"), state.proposals.map { it.item.title })
        assertEquals(listOf("ben"), state.proposals[0].item.faces.map { it.id })
        assertEquals("Milk", state.proposals[1].item.summary)
        assertEquals(0, state.acceptedCount)
        assertTrue(state.canAcceptAll)
    }

    @Test
    fun theCheckAcceptsAProposalAndTakesItBack() = runTest {
        val plan = draft()
        val viewModel = board(plan)

        viewModel.toggleAccepted("proposal-0")
        assertEquals(listOf(true, false), viewModel.uiState.value.proposals.map { it.accepted })
        assertEquals(listOf("proposal-0"), plan.review.value?.accepted?.map { it.id })

        viewModel.toggleAccepted("proposal-0")
        assertEquals(0, viewModel.uiState.value.acceptedCount)
    }

    @Test
    fun acceptAllAcceptsTheRest() = runTest {
        val viewModel = board(draft())
        viewModel.toggleAccepted("proposal-1")

        viewModel.acceptAll()

        assertEquals(2, viewModel.uiState.value.acceptedCount)
        assertFalse(viewModel.uiState.value.canAcceptAll)
    }

    @Test
    fun aNewSpaceShowsItsProposedNameWithTheUserAsItsOnlyMember() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val state = board(draft(SpaceProposal("New flat", accent = 2)), spaceId = "").uiState.value

        assertEquals("New flat", state.spaceName)
        assertEquals(2, state.proposals.size)
        // Ben is not in a space that does not exist yet.
        assertTrue(state.proposals[0].item.faces.isEmpty())
    }

    @Test
    fun withoutAPlanThereIsNothingToReview() = runTest {
        val state = board(PlanDraft()).uiState.value

        assertTrue(state.missing)
        assertTrue(state.empty)
    }

    @Test
    fun removingEveryProposalLeavesTheBoardEmpty() = runTest {
        val plan = draft()
        val viewModel = board(plan)

        plan.update { it.remove("proposal-0").remove("proposal-1") }

        assertTrue(viewModel.uiState.value.empty)
        assertFalse(viewModel.uiState.value.missing)
    }

    @Test
    fun failedReadsOnlyLoseTheDecoration() = runTest {
        val state = board(draft(), spaces = FlakySpaceRepository(spaceFailures = 1, memberFailures = 1)).uiState.value

        assertEquals(null, state.spaceName)
        assertEquals(listOf("Bins", "Groceries"), state.proposals.map { it.item.title })
        assertTrue(state.proposals[0].item.faces.isEmpty())
    }
}
