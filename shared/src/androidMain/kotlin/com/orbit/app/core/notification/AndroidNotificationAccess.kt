package com.orbit.app.core.notification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** Reads the notification switch through [NotificationManagerCompat], which also covers the Android 13 permission. */
class AndroidNotificationAccess(private val context: Context) : NotificationAccess {
    override suspend fun isAllowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Started from the application context, so the settings open in a new task. */
    override fun openSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
