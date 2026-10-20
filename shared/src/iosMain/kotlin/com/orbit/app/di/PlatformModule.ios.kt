package com.orbit.app.di

import com.orbit.app.core.notification.foregroundNotifications
import com.orbit.app.core.notification.IosNotificationAccess
import com.orbit.app.core.notification.IosNotificationScheduler
import com.orbit.app.core.notification.NotificationAccess
import com.orbit.app.core.notification.NotificationScheduler
import com.orbit.app.core.share.IosTextSharer
import com.orbit.app.core.share.TextSharer
import com.orbit.app.data.auth.GoogleAuthClient
import com.orbit.app.data.auth.GoogleSignInBridge
import com.orbit.app.data.auth.IosGoogleAuthClient
import org.koin.dsl.module
import platform.UserNotifications.UNUserNotificationCenter

actual val platformModule = module {
    single<GoogleAuthClient> { IosGoogleAuthClient(get()) }
    single<TextSharer> { IosTextSharer() }
    single<NotificationScheduler> { IosNotificationScheduler() }
    single<NotificationAccess> { IosNotificationAccess() }
}

/** Starts Koin from Swift with the parts only Swift can provide. */
fun initKoin(googleSignIn: GoogleSignInBridge) {
    // Set at launch, before any notification can arrive.
    UNUserNotificationCenter.currentNotificationCenter().delegate = foregroundNotifications
    initKoin {
        modules(module { single { googleSignIn } })
    }
}
