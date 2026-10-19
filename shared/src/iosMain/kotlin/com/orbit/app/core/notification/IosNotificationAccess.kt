package com.orbit.app.core.notification

import co.touchlab.kermit.Logger

/** Stub until #204 reads UNUserNotificationCenter: notifications count as off on iOS. */
class IosNotificationAccess : NotificationAccess {
    override suspend fun isAllowed(): Boolean = false

    override fun openSettings() {
        Logger.w { "Notification settings not opened: iOS notifications land in #204" }
    }
}
