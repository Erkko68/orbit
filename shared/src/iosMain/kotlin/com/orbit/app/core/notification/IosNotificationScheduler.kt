package com.orbit.app.core.notification

import co.touchlab.kermit.Logger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock
import kotlin.time.Instant

/** Schedules through [UNUserNotificationCenter], which keeps pending notifications across reboots on its own. */
class IosNotificationScheduler : NotificationScheduler {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override fun schedule(id: String, at: Instant, title: String, body: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        // The trigger needs a positive interval: a time already past shows it at once, as on Android.
        val seconds = (at - Clock.System.now()).inWholeSeconds.coerceAtLeast(1)
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds.toDouble(), repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(id, content, trigger)
        center.addNotificationRequest(request) { error ->
            error?.let { Logger.e { "Notification $id not scheduled: ${it.localizedDescription}" } }
        }
    }

    override fun cancel(id: String) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(id))
    }
}
