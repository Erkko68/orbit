package com.orbit.app.feature.item.history

import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ItemHistoryViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val items = InMemoryItemRepository(auth)
    private val spaces = FakeSpaceRepository(
        Space("space-1", "Flat 4B", 0, listOf("ana@orbit.app", "ben@orbit.app")),
        listOf(
            Member("ana@orbit.app", "Ana Lopez", null, Role.OWNER),
            Member("ben@orbit.app", "Ben", "https://photo", Role.MEMBER),
        ),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.historyViewModel(repository: ItemRepository, itemId: String = "item-1") =
        ItemHistoryViewModel("space-1", itemId, repository, spaces, auth).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    private fun completions(vararg completions: Completion) = object : ItemRepository by items {
        override fun observeCompletions(spaceId: String, itemId: String): Flow<List<Completion>> = flowOf(completions.toList())
    }

    private val evening = LocalDateTime(2026, 11, 3, 19, 45, 30)
    private fun completion(id: String, by: String) =
        Completion(id, "item-1", by, evening.toInstant(TimeZone.currentSystemDefault()))

    @Test
    fun namesWhoCompletedItAndWhenOnThePhonesClock() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = historyViewModel(completions(completion("c2", "ben@orbit.app"), completion("c1", "ana@orbit.app")))

        val (ben, ana) = viewModel.uiState.value.completions
        assertEquals(ItemCompletion("c2", "Ben", false, "B", "https://photo", 1, evening.date, LocalTime(19, 45)), ben)
        assertTrue(ana.isYou)
        assertEquals("AL", ana.initials)
        assertEquals(0, ana.index)
    }

    @Test
    fun someoneWhoLeftTheSpaceHasNoName() = runTest {
        val viewModel = historyViewModel(completions(completion("c1", "cy@orbit.app")))

        val cy = viewModel.uiState.value.completions.single()
        assertEquals(null, cy.name)
        assertEquals(0, cy.index)
    }

    @Test
    fun completingTheItemAddsToItsHistory() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = items.create("space-1", NewItem("Bins"))
        val viewModel = historyViewModel(items, id)
        assertFalse(viewModel.uiState.value.hasHistory)

        items.complete("space-1", id)
        items.reopen("space-1", id)
        items.complete("space-1", id)

        assertEquals(2, viewModel.uiState.value.completions.size)
    }

    @Test
    fun aFailedReadShowsNoHistory() = runTest {
        val failing = object : ItemRepository by items {
            override fun observeCompletions(spaceId: String, itemId: String): Flow<List<Completion>> =
                flow { throw IllegalStateException("Read failed") }
        }

        assertFalse(historyViewModel(failing).uiState.value.hasHistory)
    }
}
