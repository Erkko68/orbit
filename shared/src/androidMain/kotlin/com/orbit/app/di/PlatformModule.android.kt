package com.orbit.app.di

import android.app.Application
import android.content.Context
import com.orbit.app.core.notification.AndroidNotificationScheduler
import com.orbit.app.core.notification.NotificationScheduler
import com.orbit.app.core.share.AndroidTextSharer
import com.orbit.app.core.share.TextSharer
import com.orbit.app.data.auth.ActivityProvider
import com.orbit.app.data.auth.AndroidGoogleAuthClient
import com.orbit.app.data.auth.GoogleAuthClient
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule = module {
    // Created with Koin, before any activity, or it misses the first one's lifecycle callbacks.
    single(createdAtStart = true) {
        val app = androidContext() as? Application
        ActivityProvider().also { provider ->
            app?.registerActivityLifecycleCallbacks(provider)
        }
    }
    single<GoogleAuthClient> {
        val provider: ActivityProvider = get()
        AndroidGoogleAuthClient(
            context = androidContext(),
            activityProvider = { provider.currentActivity },
        )
    }
    single<TextSharer> { AndroidTextSharer(androidContext()) }
    single {
        AndroidNotificationScheduler(
            context = androidContext(),
            store = SharedPreferencesSettings(
                androidContext().getSharedPreferences("notifications", Context.MODE_PRIVATE),
            ),
        )
    } bind NotificationScheduler::class
}
