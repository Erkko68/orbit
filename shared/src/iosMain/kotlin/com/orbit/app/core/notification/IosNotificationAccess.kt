package com.orbit.app.core.notification

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenNotificationSettingsURLString
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Reads the notification settings of [UNUserNotificationCenter]: provisional and ephemeral count as allowed. */
class IosNotificationAccess : NotificationAccess {
    override suspend fun isAllowed(): Boolean = suspendCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            continuation.resume(status != UNAuthorizationStatusDenied && status != UNAuthorizationStatusNotDetermined)
        }
    }

    /** Opens the app's own notification page in the Settings app. */
    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenNotificationSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}
