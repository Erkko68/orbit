package com.orbit.app.feature.activity

import com.orbit.app.domain.model.Activity
import com.orbit.app.domain.model.ActivityAction
import com.orbit.app.domain.model.ActivityTarget
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryActivityRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val history = InMemoryActivityRepository()
    private val now = Clock.System.now()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Ana owns Flat 4B and Ben is a member of it. Ana is signed in. */
    private suspend fun flat() {
        auth.signIn("ana@orbit.app", "secret")
        spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember("space-1", Member("ben@orbit.app", "Ben", photoUrl = null, role = Role.MEMBER))
    }

    private fun entry(
        id: String,
        ago: Duration,
        by: String = "ben@orbit.app",
        action: ActivityAction = ActivityAction.CREATE,
        byAssistant: Boolean = false,
    ) = Activity(id, by, now - ago, action, ActivityTarget.ITEM, "item-$id", "Bins $id", byAssistant = byAssistant)

    private fun TestScope.screen(
        repository: InMemoryActivityRepository = history,
        spaceRepository: SpaceRepository = spaces,
    ) = ActivityViewModel("space-1", repository, spaceRepository, auth).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun groupsTheHistoryByDayNewestFirstAndNamesWhoDidEach() = runTest {
        flat()
        history.record("space-1", entry("old", ago = 2.days))
        history.record("space-1", entry("new", ago = 1.minutes, by = "ana@orbit.app", action = ActivityAction.COMPLETE))

        val state = screen().uiState.value

        assertFalse(state.loading)
        val days = state.days
        assertEquals(listOf("new", "old"), days.flatMap { day -> day.entries.map { it.id } })
        assertEquals(days.sortedByDescending { it.date }, days)
        val newest = days.first().entries.first()
        assertEquals(ActivityAction.COMPLETE, newest.action)
        assertTrue(newest.byYou)
        val oldest = days.last().entries.last()
        assertEquals("Ben", oldest.actor)
        assertFalse(oldest.byYou)
        assertEquals("Bins old", oldest.title)
    }

    @Test
    fun namesTodayAndYesterday() = runTest {
        flat()
        val zone = TimeZone.currentSystemDefault()
        history.record("space-1", entry("today", ago = Duration.ZERO))
        history.record("space-1", entry("yesterday", ago = 1.days))
        history.record("space-1", entry("earlier", ago = 3.days))

        val days = screen().uiState.value.days

        assertEquals(listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.EARLIER), days.map { it.relative })
        assertEquals((now - 3.days).toLocalDateTime(zone).date, days.last().date)
    }

    @Test
    fun marksWhatTheAssistantDid() = runTest {
        flat()
        history.record("space-1", entry("plan", ago = 1.minutes, byAssistant = true))
        history.record("space-1", entry("hand", ago = 2.minutes))

        val entries = screen().uiState.value.days.single().entries

        assertEquals(listOf(true, false), entries.map { it.byAssistant })
    }

    @Test
    fun readsOnlyTheLast30Days() = runTest {
        flat()
        history.record("space-1", entry("recent", ago = 29.days))
        history.record("space-1", entry("too-old", ago = 31.days))

        val ids = screen().uiState.value.days.flatMap { day -> day.entries.map { it.id } }

        assertEquals(listOf("recent"), ids)
    }

    @Test
    fun namesAFormerMemberAndLeavesSomeoneUnknownWithoutAName() = runTest {
        flat()
        spaces.removeMember("space-1", "ben@orbit.app", "Ben")
        history.record("space-1", entry("left", ago = 1.minutes))
        history.record("space-1", entry("stranger", ago = 2.minutes, by = "zoe@orbit.app"))

        val entries = screen().uiState.value.days.single().entries

        assertEquals("Ben", entries.first { it.id == "left" }.actor)
        assertNull(entries.first { it.id == "stranger" }.actor)
    }

    @Test
    fun anEmptyHistorySaysSo() = runTest {
        flat()

        val state = screen().uiState.value

        assertTrue(state.isEmpty)
    }

    @Test
    fun aFailedReadShowsWithARetryThatLoadsIt() = runTest {
        flat()
        val flaky = InMemoryActivityRepository(failures = 1)
        flaky.record("space-1", entry("back", ago = 1.minutes))
        val viewModel = screen(repository = flaky)

        assertTrue(viewModel.uiState.value.loadFailed)
        assertFalse(viewModel.uiState.value.isEmpty)

        viewModel.retry()

        assertFalse(viewModel.uiState.value.loadFailed)
        assertEquals("back", viewModel.uiState.value.days.single().entries.single().id)
    }

    @Test
    fun theHistoryStillShowsWhenTheMembersFailToLoad() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        history.record("space-1", entry("one", ago = 1.minutes, by = "ana"))

        val state = screen(spaceRepository = FlakySpaceRepository(spaceFailures = 1, memberFailures = 1)).uiState.value

        val shown = state.days.single().entries.single()
        assertFalse(state.loadFailed)
        assertNull(shown.actor)
    }
}
