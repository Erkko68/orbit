package com.orbit.app.feature.member

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.role_admin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MemberViewModelTest {
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

    private fun TestScope.memberViewModel(
        userId: String,
        itemRepository: ItemRepository = items,
        spaceRepository: SpaceRepository = spaces,
    ) = MemberViewModel("space-1", userId, itemRepository, spaceRepository, auth).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun bens(title: String, date: LocalDate? = null) = NewItem(
        title,
        modules = ItemModules(schedule = date?.let { Schedule(it) }, assignment = Assignment.ToMembers(setOf("ben@orbit.app"))),
    )

    @Test
    fun showsWhoTheyAreInTheSpace() = runTest {
        flat()

        val state = memberViewModel("ben@orbit.app").uiState.value

        assertTrue(state.loaded)
        assertEquals("Ben Ruiz", state.name)
        assertEquals("BR", state.initials)
        assertEquals(Res.string.role_admin, state.role)
        assertEquals(1, state.index)
        assertEquals("Flat 4B", state.spaceName)
        assertTrue(state.isYou)
    }

    @Test
    fun listsTheirOpenItemsTheSoonestFirst() = runTest {
        flat()
        items.create("space-1", bens("Recycling", LocalDate(2026, 11, 12)))
        items.create("space-1", bens("Internet", LocalDate(2026, 11, 6)))
        items.create("space-1", bens("Someday"))
        items.create("space-1", NewItem("Somebody else's"))
        val done = items.create("space-1", bens("Already done"))
        items.complete("space-1", done)

        val state = memberViewModel("ben@orbit.app").uiState.value

        assertEquals(listOf("Internet", "Recycling", "Someday"), state.has.map { it.title })
        assertEquals(LocalDate(2026, 11, 6), state.has.first().due)
    }

    @Test
    fun listsWhatTheyCompletedLatelyAndSkipsDeletedItems() = runTest {
        flat()
        val kitchen = items.create("space-1", NewItem("Clean the kitchen"))
        val gone = items.create("space-1", NewItem("Gone"))
        items.complete("space-1", kitchen)
        items.complete("space-1", gone)
        items.delete("space-1", gone)

        val state = memberViewModel("ben@orbit.app").uiState.value

        assertEquals(listOf("Clean the kitchen"), state.lately.map { it.title })
        assertEquals(kitchen, state.lately.single().itemId)
        assertFalse(state.hasNothing)
    }

    @Test
    fun someoneWhoLeftIsNotFound() = runTest {
        flat()

        val state = memberViewModel("cy@orbit.app").uiState.value

        assertTrue(state.notFound)
        assertFalse(state.loaded)
    }

    @Test
    fun saysWhenTheMembersFailToLoadAndLoadsThemOnRetry() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val viewModel = memberViewModel("ana@orbit.app", spaceRepository = FlakySpaceRepository(memberFailures = 1))
        assertTrue(viewModel.uiState.value.loadFailed)

        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
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

        val state = memberViewModel("ben@orbit.app", itemRepository = failing).uiState.value

        assertTrue(state.loaded)
        assertTrue(state.hasNothing)
    }
}
