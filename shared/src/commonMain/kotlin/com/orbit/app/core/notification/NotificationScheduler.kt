package com.orbit.app.core.notification

import kotlin.time.Instant

/** Local notifications the system shows at a set time, even when the app is closed. */
interface NotificationScheduler {
    /** Shows [title] and [body] at [at]. Scheduling an [id] again replaces its notification. */
    fun schedule(id: String, at: Instant, title: String, body: String)

    /** Drops the notification [id], whether it is still pending or already shown. */
    fun cancel(id: String)
}
