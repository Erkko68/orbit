package com.orbit.app.core.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.orbit.app.core.designsystem.ComposeTest
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NotificationPermissionPromptTest : ComposeTest() {
    private val answers = mutableListOf<Boolean>()

    @Test
    fun theUserAllowsNotifications() {
        assertEquals(listOf(true), answer(PackageManager.PERMISSION_GRANTED))
    }

    @Test
    fun theUserRefusesNotifications() {
        assertEquals(listOf(false), answer(PackageManager.PERMISSION_DENIED))
    }

    @Suppress("DEPRECATION")
    private fun answer(result: Int): List<Boolean> {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        lateinit var prompt: () -> Unit
        activity.setContent { prompt = rememberNotificationPermissionPrompt { answers += it } }
        shadowOf(Looper.getMainLooper()).idle()

        prompt()

        val request = assertNotNull(shadowOf(activity).lastRequestedPermission)
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), request.requestedPermissions.toList())
        activity.onRequestPermissionsResult(request.requestCode, request.requestedPermissions, intArrayOf(result))
        shadowOf(Looper.getMainLooper()).idle()
        return answers
    }
}
