package com.orbit.app.feature.member.activity

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MemberActivityViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Ana owns Flat 4B and Ben is an admin of it. Ben is signed in. */
    private suspend fun flat() {
        auth.signIn("ana@orbit.app", "secret")
        spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember("space-1", Member("ben@orbit.app", "Ben Ruiz", photoUrl = null, role = Role.ADMIN))
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")
    }

    private fun TestScope.activity(userId: String, itemRepository: ItemRepository = items) =
        MemberActivityViewModel("space-1", userId, itemRepository).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    private fun bens(title: String, date: LocalDate? = null) = NewItem(
        title,
        modules = ItemModules(schedule = date?.let { Schedule(it) }, assignment = Assignment.ToMembers(setOf("ben@orbit.app"))),
    )

    @Test
    fun listsTheirOpenItemsTheSoonestFirst() = runTest {
        flat()
        items.create("space-1", bens("Recycling", LocalDate(2026, 11, 12)))
        items.create("space-1", bens("Internet", LocalDate(2026, 11, 6)))
        items.create("space-1", bens("Someday"))
        items.create("space-1", NewItem("Somebody else's"))
        val done = items.create("space-1", bens("Already done"))
        items.complete("space-1", done, null)

        val state = activity("ben@orbit.app").uiState.value

        assertEquals(listOf("Internet", "Recycling", "Someday"), state.has.map { it.title })
        assertEquals(LocalDate(2026, 11, 6), state.has.first().due)
    }

    @Test
    fun listsWhatTheyCompletedLatelyAndSkipsDeletedItems() = runTest {
        flat()
        val kitchen = items.create("space-1", NewItem("Clean the kitchen"))
        val gone = items.create("space-1", NewItem("Gone"))
        items.complete("space-1", kitchen, null)
        items.complete("space-1", gone, null)
        items.delete("space-1", gone)

        val state = activity("ben@orbit.app").uiState.value

        assertEquals(listOf("Clean the kitchen"), state.lately.map { it.title })
        assertEquals(kitchen, state.lately.single().itemId)
        assertFalse(state.hasNothing)
    }

    @Test
    fun showsWhatTheyHaveBeforeTheirHistoryArrives() = runTest {
        flat()
        items.create("space-1", bens("Recycling"))
        val slow = object : ItemRepository by items {
            override fun observeMemberCompletions(spaceId: String, userId: String): Flow<List<Completion>> =
                MutableSharedFlow()
        }

        val state = activity("ben@orbit.app", slow).uiState.value

        assertFalse(state.loading)
        assertEquals(listOf("Recycling"), state.has.map { it.title })
    }

    @Test
    fun aFailedReadOfTheirItemsOrHistoryLeavesThemEmpty() = runTest {
        flat()
        items.create("space-1", bens("Recycling"))
        val failing = object : ItemRepository by items {
            override fun observeMemberCompletions(spaceId: String, userId: String): Flow<List<Completion>> =
                flow { error("Read failed") }

            override fun observeItems(spaceId: String) = flow<List<Item>> { error("Read failed") }
        }

        val state = activity("ben@orbit.app", failing).uiState.value

        assertFalse(state.loading)
        assertTrue(state.hasNothing)
    }
}
