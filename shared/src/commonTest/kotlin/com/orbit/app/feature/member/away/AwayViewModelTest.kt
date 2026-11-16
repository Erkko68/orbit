package com.orbit.app.feature.member.away

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import com.orbit.app.domain.repository.SpaceRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class AwayViewModelTest {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.awayViewModel(spaces: SpaceRepository) =
        AwayViewModel("space-1", "ana", spaces).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    private fun team(writeFailure: Exception? = null) =
        FakeSpaceRepository.withMembers("ana" to Role.OWNER, "ben" to Role.MEMBER, writeFailure = writeFailure)

    private fun millisOf(day: kotlinx.datetime.LocalDate) = day.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    @Test
    fun `a member who is not away has no period and the sheet opens on today`() = runTest {
        val viewModel = awayViewModel(team())
        assertFalse(viewModel.uiState.value.loading)
        assertFalse(viewModel.uiState.value.isAway)
        viewModel.open()
        assertTrue(viewModel.uiState.value.editing)
        assertEquals(today, viewModel.uiState.value.draftFrom)
        assertEquals(today, viewModel.uiState.value.draftUntil)
    }

    @Test
    fun `saving writes the days and closes the sheet`() = runTest {
        val viewModel = awayViewModel(team())
        viewModel.open()
        viewModel.pick(AwayEnd.UNTIL)
        viewModel.pickDate(millisOf(today.plus(DatePeriod(days = 5))))
        viewModel.save()
        val state = viewModel.uiState.value
        assertFalse(state.editing)
        assertEquals(today, state.from)
        assertEquals(today.plus(DatePeriod(days = 5)), state.until)
        assertFalse(state.failed)
    }

    @Test
    fun `a first day after the last one moves the last one with it`() = runTest {
        val viewModel = awayViewModel(team())
        viewModel.open()
        viewModel.pick(AwayEnd.FROM)
        viewModel.pickDate(millisOf(today.plus(DatePeriod(days = 3))))
        assertEquals(today.plus(DatePeriod(days = 3)), viewModel.uiState.value.draftUntil)
        assertNull(viewModel.uiState.value.picking)
        assertTrue(viewModel.uiState.value.canSave)
    }

    @Test
    fun `the last day cannot be picked before the first`() = runTest {
        val viewModel = awayViewModel(team())
        viewModel.open()
        viewModel.pick(AwayEnd.FROM)
        viewModel.pickDate(millisOf(today.plus(DatePeriod(days = 4))))
        viewModel.pick(AwayEnd.UNTIL)
        assertEquals(millisOf(today.plus(DatePeriod(days = 4))), viewModel.uiState.value.earliestMillis)
    }

    @Test
    fun `a failed save keeps the sheet open and says so until the next edit`() = runTest {
        val viewModel = awayViewModel(team(writeFailure = IllegalStateException("denied")))
        viewModel.open()
        viewModel.save()
        assertTrue(viewModel.uiState.value.editing)
        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.busy)
        assertFalse(viewModel.uiState.value.isAway)
        viewModel.pick(AwayEnd.UNTIL)
        assertFalse(viewModel.uiState.value.failed)
    }

    @Test
    fun `clearing removes the period`() = runTest {
        val spaces = team()
        val viewModel = awayViewModel(spaces)
        spaces.setAway("space-1", today, today.plus(DatePeriod(days = 2)))
        assertTrue(viewModel.uiState.value.isAway)
        viewModel.open()
        viewModel.clear()
        assertFalse(viewModel.uiState.value.isAway)
        assertFalse(viewModel.uiState.value.editing)
    }

    @Test
    fun `a failed clear keeps the period and says so`() = runTest {
        val away = Member("ana", "ana", null, Role.OWNER, today, today.plus(DatePeriod(days = 2)))
        val spaces = FakeSpaceRepository(
            Space("space-1", "Flat 4B", accent = 0, memberIds = listOf("ana")),
            listOf(away),
            writeFailure = IllegalStateException("denied"),
        )
        val viewModel = awayViewModel(spaces)
        viewModel.open()
        viewModel.clear()
        assertTrue(viewModel.uiState.value.isAway)
        assertTrue(viewModel.uiState.value.failed)
        assertFalse(viewModel.uiState.value.busy)
        assertTrue(viewModel.uiState.value.editing)
    }

    @Test
    fun `a period that has passed is not shown`() = runTest {
        val spaces = team()
        spaces.setAway("space-1", today.plus(DatePeriod(days = -10)), today.plus(DatePeriod(days = -5)))
        val viewModel = awayViewModel(spaces)
        assertFalse(viewModel.uiState.value.isAway)
    }

    @Test
    fun `a failed read shows the failure and a retry loads the member`() = runTest {
        val viewModel = awayViewModel(FlakySpaceRepository(memberFailures = 1))
        assertTrue(viewModel.uiState.value.loadFailed)
        viewModel.retry()
        assertFalse(viewModel.uiState.value.loadFailed)
        assertFalse(viewModel.uiState.value.loading)
    }
}
