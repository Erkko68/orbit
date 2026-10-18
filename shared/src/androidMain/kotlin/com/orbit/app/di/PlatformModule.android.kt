package com.orbit.app.di

import android.app.Application
import com.orbit.app.data.auth.ActivityProvider
import com.orbit.app.data.auth.AndroidGoogleAuthClient
import com.orbit.app.data.auth.GoogleAuthClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single {
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
}
