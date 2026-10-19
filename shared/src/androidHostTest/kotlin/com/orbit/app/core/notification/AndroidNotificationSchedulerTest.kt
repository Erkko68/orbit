package com.orbit.app.core.notification

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import com.orbit.app.core.designsystem.ComposeTest
import com.russhwolf.settings.MapSettings
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class AndroidNotificationSchedulerTest : ComposeTest() {
    private val app = RuntimeEnvironment.getApplication()
    private val alarms = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val notifications = shadowOf(app.getSystemService(NotificationManager::class.java))
    private val store = MapSettings()
    private val scheduler = AndroidNotificationScheduler(app, store)
    private val at = Instant.parse("2026-11-02T18:00:00Z")

    @BeforeTest
    fun setUp() {
        // The receivers find the scheduler through Koin, as in the app.
        startKoin { modules(module { single { scheduler } }) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun schedulesAnAlarmThatFiresInDozeAtTheInstant() {
        scheduler.schedule("bins", at, "Take out the bins", "Flat 4B")

        val alarm = alarms.scheduledAlarms.single()
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertEquals(at.toEpochMilliseconds(), alarm.triggerAtMs)
        assertTrue(alarm.isAllowWhileIdle)
    }

    @Test
    fun schedulingAnIdAgainReplacesItsAlarm() {
        scheduler.schedule("bins", at, "Take out the bins", "Flat 4B")
        scheduler.schedule("rent", at, "Pay the rent", "Flat 4B")
        val later = Instant.parse("2026-11-03T08:00:00Z")
        scheduler.schedule("bins", later, "Take out the bins", "Flat 4B")

        assertEquals(
            listOf(at.toEpochMilliseconds(), later.toEpochMilliseconds()),
            alarms.scheduledAlarms.map { it.triggerAtMs }.sorted(),
        )
    }

    @Test
    fun theAlarmShowsTheNotification() {
        scheduler.schedule("bins", at, "Take out the bins", "Flat 4B")

        fireNextAlarm()

        val notification = assertNotNull(notifications.getNotification("bins", 0))
        assertEquals("Take out the bins", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Flat 4B", notification.extras.getString(Notification.EXTRA_TEXT))
        assertEquals("Reminders", notifications.notificationChannels.single().name)
    }

    @Test
    fun nothingShowsWhenTheUserTurnedNotificationsOff() {
        notifications.setNotificationsEnabled(false)
        scheduler.schedule("bins", at, "Take out the bins", "Flat 4B")

        fireNextAlarm()

        assertEquals(0, notifications.size())
    }

    @Test
    fun cancelDropsThePendingAlarm() {
        scheduler.schedule("bins", at, "Take out the bins", "Flat 4B")
        scheduler.schedule("rent", at, "Pay the rent", "Flat 4B")

        scheduler.cancel("bins")

        val alarm = alarms.scheduledAlarms.single()
        assertEquals("rent", shadowOf(alarm.operation).savedIntent.data?.schemeSpecificPart)
        assertEquals(setOf("rent"), store.keys)
    }

    @Test
    fun cancelDismissesAShownNotification() {
        scheduler.schedule("bins", at, "Take out the bins", "Flat 4B")
        fireNextAlarm()

        scheduler.cancel("bins")

        assertNull(notifications.getNotification("bins", 0))
    }

    @Test
    fun aRebootSetsThePendingAlarmsAgain() {
        scheduler.schedule("bins", at, "Take out the bins", "Flat 4B")
        scheduler.schedule("rent", Instant.parse("2026-11-03T08:00:00Z"), "Pay the rent", "Flat 4B")
        fireNextAlarm()
        // A reboot drops every alarm.
        alarms.scheduledAlarms.forEach { app.getSystemService(AlarmManager::class.java).cancel(it.operation!!) }
        assertTrue(alarms.scheduledAlarms.isEmpty())

        BootReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        val alarm = alarms.scheduledAlarms.single()
        assertEquals("rent", shadowOf(alarm.operation).savedIntent.data?.schemeSpecificPart)
    }

    private fun fireNextAlarm() {
        alarms.fireAlarm(alarms.peekNextScheduledAlarm())
        shadowOf(Looper.getMainLooper()).idle()
    }
}
