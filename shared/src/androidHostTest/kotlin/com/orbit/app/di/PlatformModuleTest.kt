package com.orbit.app.di

import android.app.Activity
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.notification.NotificationAccess
import com.orbit.app.core.notification.NotificationScheduler
import com.orbit.app.data.auth.ActivityProvider
import com.orbit.app.data.auth.GoogleAuthClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class PlatformModuleTest : ComposeTest() {

    @Test
    fun platformModuleWiresDependencies() {
        val app = RuntimeEnvironment.getApplication()
        // Closed after: platformModule is a shared val, so its singles outlive an open app into the next test.
        val koinApp = koinApplication {
            androidContext(app)
            modules(platformModule)
        }
        val koin = koinApp.koin

        val provider = koin.get<ActivityProvider>()
        assertNotNull(provider)

        val client = koin.get<GoogleAuthClient>()
        assertNotNull(client)

        assertNotNull(koin.get<NotificationScheduler>())
        assertNotNull(koin.get<NotificationAccess>())
        koinApp.close()
    }

    @Test
    fun activityProviderSeesTheFirstActivityBeforeAnyoneAsksForIt() {
        val koin = startKoin {
            androidContext(RuntimeEnvironment.getApplication())
            modules(platformModule)
        }.koin
        try {
            val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

            assertSame(activity, koin.get<ActivityProvider>().currentActivity)
        } finally {
            stopKoin()
        }
    }
}
