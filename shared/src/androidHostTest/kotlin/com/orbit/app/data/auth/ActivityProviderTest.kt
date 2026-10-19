package com.orbit.app.data.auth

import android.app.Activity
import android.os.Bundle
import com.orbit.app.core.designsystem.ComposeTest
import org.robolectric.Robolectric
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActivityProviderTest : ComposeTest() {

    @Test
    fun tracksActivityLifecycle() {
        val provider = ActivityProvider()
        assertNull(provider.currentActivity)

        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()

        provider.onActivityCreated(activity, Bundle())
        assertEquals(activity, provider.currentActivity)

        provider.onActivityStarted(activity)
        assertEquals(activity, provider.currentActivity)

        provider.onActivityResumed(activity)
        assertEquals(activity, provider.currentActivity)

        provider.onActivityPaused(activity)
        assertEquals(activity, provider.currentActivity)

        provider.onActivityStopped(activity)
        assertEquals(activity, provider.currentActivity)

        provider.onActivitySaveInstanceState(activity, Bundle())
        assertEquals(activity, provider.currentActivity)

        provider.onActivityDestroyed(activity)
        assertNull(provider.currentActivity)
    }

    @Test
    fun doesNotClearWhenAnotherActivityIsDestroyed() {
        val provider = ActivityProvider()
        val controller1 = Robolectric.buildActivity(Activity::class.java).setup()
        val activity1 = controller1.get()
        val controller2 = Robolectric.buildActivity(Activity::class.java).setup()
        val activity2 = controller2.get()

        provider.onActivityResumed(activity1)
        assertEquals(activity1, provider.currentActivity)

        provider.onActivityDestroyed(activity2)
        assertEquals(activity1, provider.currentActivity)
    }
}
