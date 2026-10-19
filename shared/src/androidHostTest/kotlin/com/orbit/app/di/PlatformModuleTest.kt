package com.orbit.app.di

import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.data.auth.ActivityProvider
import com.orbit.app.data.auth.GoogleAuthClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertNotNull

class PlatformModuleTest : ComposeTest() {

    @Test
    fun platformModuleWiresDependencies() {
        val app = RuntimeEnvironment.getApplication()
        val koin = koinApplication {
            androidContext(app)
            modules(platformModule)
        }.koin

        val provider = koin.get<ActivityProvider>()
        assertNotNull(provider)

        val client = koin.get<GoogleAuthClient>()
        assertNotNull(client)
    }
}
