package com.orbit.app.core.notification

/** Whether the user lets the app notify, and the way to the system settings where they change it. */
interface NotificationAccess {
    suspend fun isAllowed(): Boolean

    /** Opens the app's notification settings: once refused, only the user can turn them on, there. */
    fun openSettings()
}
