package com.orbit.app.core.notification

import co.touchlab.kermit.Logger
import kotlin.time.Instant

/** Stub until #204 schedules through UNUserNotificationCenter: nothing is shown on iOS yet. */
class IosNotificationScheduler : NotificationScheduler {
    override fun schedule(id: String, at: Instant, title: String, body: String) {
        Logger.w { "Notification $id not scheduled: iOS notifications land in #204" }
    }

    override fun cancel(id: String) {
        Logger.w { "Notification $id not cancelled: iOS notifications land in #204" }
    }
}
