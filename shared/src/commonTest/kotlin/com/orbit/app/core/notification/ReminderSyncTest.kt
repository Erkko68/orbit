package com.orbit.app.core.notification

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FlakySpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import com.orbit.app.testing.InMemorySpaceRepository
import com.orbit.app.testing.RecordingNotificationScheduler
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSyncTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)
    private val scheduler = RecordingNotificationScheduler()
    private val settings = MapSettings()

    private val day = LocalDate(2099, 1, 2)
    private val zone = TimeZone.currentSystemDefault()
    private val dinner = NewItem(
        title = "Dinner",
        modules = ItemModules(
            schedule = Schedule(day, LocalTime(20, 0)),
            reminder = Reminder(listOf(30.minutes)),
        ),
    )

    private fun TestScope.startSync(spaces: SpaceRepository = this@ReminderSyncTest.spaces): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ReminderSync(auth, spaces, items, scheduler, settings).run()
        }

    private suspend fun signedInSpace(): String {
        auth.signIn("ana@orbit.app", "secret")
        return spaces.createSpace("Flat 4B", accent = 0)
    }

    @Test
    fun schedulesBeforeTheDueMoment() = runTest {
        val spaceId = signedInSpace()
        val itemId = items.create(spaceId, dinner)
        startSync()

        val pending = scheduler.pending.getValue("reminder-$itemId-30")
        assertEquals(day.atTime(19, 30).toInstant(zone), pending.at)
        assertEquals("Dinner", pending.title)
        assertEquals("Flat 4B", pending.body)
    }

    @Test
    fun anAllDayItemRemindsAtNine() = runTest {
        val spaceId = signedInSpace()
        val itemId = items.create(spaceId, dinner.copy(modules = dinner.modules.with(Schedule(day))))
        startSync()

        assertEquals(day.atTime(8, 30).toInstant(zone), scheduler.pending.getValue("reminder-$itemId-30").at)
    }

    @Test
    fun completingCancelsAndReopeningSchedulesAgain() = runTest {
        val spaceId = signedInSpace()
        val itemId = items.create(spaceId, dinner)
        startSync()

        items.complete(spaceId, itemId)
        assertTrue(scheduler.pending.isEmpty())

        items.reopen(spaceId, itemId)
        assertEquals(setOf("reminder-$itemId-30"), scheduler.pending.keys)
    }

    @Test
    fun editingTheScheduleReschedules() = runTest {
        val spaceId = signedInSpace()
        val itemId = items.create(spaceId, dinner)
        startSync()

        items.setModule(spaceId, itemId, Schedule(day, LocalTime(21, 0)))

        assertEquals(day.atTime(20, 30).toInstant(zone), scheduler.pending.getValue("reminder-$itemId-30").at)
    }

    @Test
    fun removingTheReminderCancels() = runTest {
        val spaceId = signedInSpace()
        val itemId = items.create(spaceId, dinner)
        startSync()

        items.removeModule(spaceId, itemId, ModuleType.REMINDER)

        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun onlyRemindsTheAssigneeOrEveryoneWhenNobodyIs() = runTest {
        val spaceId = signedInSpace()
        fun assigned(assignment: Assignment) = dinner.copy(modules = dinner.modules.with(assignment))
        val mine = items.create(spaceId, assigned(Assignment.ToMember("ana@orbit.app")))
        val theirs = items.create(spaceId, assigned(Assignment.ToMember("bo@orbit.app")))
        val unclaimed = items.create(spaceId, assigned(Assignment.Claim()))
        startSync()

        assertEquals(setOf("reminder-$mine-30", "reminder-$unclaimed-30"), scheduler.pending.keys)
        assertTrue("reminder-$theirs-30" !in scheduler.pending)
    }

    @Test
    fun aPastReminderIsNotScheduled() = runTest {
        val spaceId = signedInSpace()
        items.create(spaceId, dinner.copy(modules = dinner.modules.with(Schedule(LocalDate(2000, 1, 1)))))
        startSync()

        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun anUnrelatedChangeSchedulesNothingAgain() = runTest {
        val spaceId = signedInSpace()
        items.create(spaceId, dinner)
        val other = items.create(spaceId, NewItem(title = "Bins"))
        startSync()
        val calls = scheduler.calls

        items.rename(spaceId, other, "Recycling")

        assertEquals(calls, scheduler.calls)
    }

    @Test
    fun signingOutCancelsEverything() = runTest {
        val spaceId = signedInSpace()
        items.create(spaceId, dinner)
        startSync()

        auth.signOut()

        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun catchesUpOnWhatChangedWhileItWasNotRunning() = runTest {
        val spaceId = signedInSpace()
        val itemId = items.create(spaceId, dinner)
        startSync().cancel()

        items.complete(spaceId, itemId)
        startSync()

        assertTrue(scheduler.pending.isEmpty())
    }

    @Test
    fun aFailedReadKeepsTheRemindersAndTriesAgain() = runTest {
        val spaceId = signedInSpace()
        val itemId = items.create(spaceId, dinner)
        startSync().cancel()

        // The flaky space is space-1, as is the one created above.
        startSync(FlakySpaceRepository(spaceFailures = 1))
        assertEquals(setOf("reminder-$itemId-30"), scheduler.pending.keys)

        items.complete(spaceId, itemId)
        assertEquals(setOf("reminder-$itemId-30"), scheduler.pending.keys)

        advanceTimeBy(31.seconds)
        runCurrent()
        assertTrue(scheduler.pending.isEmpty())
    }
}
