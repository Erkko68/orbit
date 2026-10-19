package com.orbit.app.core.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.orbit.app.shared.R
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/**
 * Schedules through [AlarmManager] and keeps every pending notification in [store], keyed by id:
 * the system drops alarms on reboot, and [BootReceiver] sets them again from there.
 */
class AndroidNotificationScheduler(
    private val context: Context,
    private val store: Settings,
) : NotificationScheduler {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = NotificationManagerCompat.from(context)

    override fun schedule(id: String, at: Instant, title: String, body: String) {
        val pending = Pending(at.toEpochMilliseconds(), title, body)
        store.putString(id, Json.encodeToString(pending))
        setAlarm(id, pending.atMillis)
    }

    override fun cancel(id: String) {
        store.remove(id)
        alarms.cancel(alarmIntent(id))
        notifications.cancel(id, NOTIFICATION_ID)
    }

    /** Sets every pending alarm again. */
    internal fun rescheduleAll() {
        store.keys.forEach { id -> pending(id)?.let { setAlarm(id, it.atMillis) } }
    }

    /** Posts the notification [id], once its alarm went off. */
    // areNotificationsEnabled() is false without POST_NOTIFICATIONS, which lint does not see.
    @SuppressLint("MissingPermission")
    internal fun show(id: String) {
        val pending = pending(id) ?: return
        store.remove(id)
        if (!notifications.areNotificationsEnabled()) return
        notifications.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_reminders))
                .build(),
        )
        val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(pending.title)
            .setContentText(pending.body)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        notifications.notify(id, NOTIFICATION_ID, notification)
    }

    // ponytail: inexact, so Android 12+ may deliver it up to an hour late. Exact alarms need
    // SCHEDULE_EXACT_ALARM, which Android 14 no longer grants on install: add it if reminders land late.
    private fun setAlarm(id: String, atMillis: Long) {
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent(id))
    }

    /** The data URI makes each id its own alarm, which the same id replaces or cancels. */
    private fun alarmIntent(id: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NotificationReceiver::class.java).setData(Uri.fromParts(SCHEME, id, null)),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun pending(id: String): Pending? =
        store.getStringOrNull(id)?.let { Json.decodeFromString<Pending>(it) }

    @Serializable
    private data class Pending(val atMillis: Long, val title: String, val body: String)

    internal companion object {
        const val SCHEME = "orbit-notification"
        private const val CHANNEL_ID = "reminders"
        private const val NOTIFICATION_ID = 0
    }
}
