package com.orbit.app.testing

import com.orbit.app.core.notification.NotificationAccess

/** Notifications the test turns on and off by hand, counting the trips to the system settings. */
class FakeNotificationAccess(var allowed: Boolean = false) : NotificationAccess {
    var settingsOpened = 0
        private set

    override suspend fun isAllowed(): Boolean = allowed

    override fun openSettings() {
        settingsOpened++
    }
}
