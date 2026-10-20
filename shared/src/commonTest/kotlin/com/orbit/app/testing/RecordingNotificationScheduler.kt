package com.orbit.app.testing

import com.orbit.app.core.notification.NotificationScheduler
import kotlin.time.Instant

/** Keeps the notifications pending by id, and counts every call, so a test sees what was scheduled and how often. */
class RecordingNotificationScheduler : NotificationScheduler {
    data class Pending(val at: Instant, val title: String, val body: String)

    val pending = mutableMapOf<String, Pending>()
    var calls = 0
        private set

    override fun schedule(id: String, at: Instant, title: String, body: String) {
        calls++
        pending[id] = Pending(at, title, body)
    }

    override fun cancel(id: String) {
        calls++
        pending.remove(id)
    }
}
