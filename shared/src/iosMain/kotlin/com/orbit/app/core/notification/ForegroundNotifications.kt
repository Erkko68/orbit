package com.orbit.app.core.notification

import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/**
 * Shows notifications while the app is in front too, as on Android: iOS hides them otherwise.
 */
internal class ForegroundNotifications : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or
                UNNotificationPresentationOptionSound,
        )
    }
}

/** Kept here because the center holds its delegate weakly. Kotlin objects cannot extend NSObject. */
internal val foregroundNotifications = ForegroundNotifications()
