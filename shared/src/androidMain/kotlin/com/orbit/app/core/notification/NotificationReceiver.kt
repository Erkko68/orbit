package com.orbit.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Shows a notification when its alarm goes off. Not exported: only our own alarms reach it. */
class NotificationReceiver : BroadcastReceiver(), KoinComponent {
    private val scheduler: AndroidNotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.data?.takeIf { it.scheme == AndroidNotificationScheduler.SCHEME }?.schemeSpecificPart
        id?.let(scheduler::show)
    }
}
