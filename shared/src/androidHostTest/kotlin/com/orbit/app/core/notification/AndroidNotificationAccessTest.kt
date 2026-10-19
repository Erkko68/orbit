package com.orbit.app.core.notification

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import com.orbit.app.core.designsystem.ComposeTest
import kotlinx.coroutines.test.runTest
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AndroidNotificationAccessTest : ComposeTest() {
    private val app = RuntimeEnvironment.getApplication()
    private val access = AndroidNotificationAccess(app)

    @Test
    fun followsTheSystemSwitch() = runTest {
        assertTrue(access.isAllowed())

        shadowOf(app.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)

        assertFalse(access.isAllowed())
    }

    @Test
    fun opensTheAppsNotificationSettings() {
        access.openSettings()

        val intent = assertNotNull(shadowOf(app).nextStartedActivity)
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(app.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
