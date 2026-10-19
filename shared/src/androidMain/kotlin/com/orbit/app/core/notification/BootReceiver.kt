package com.orbit.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Sets the pending notifications' alarms again, which the system drops on reboot. */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val scheduler: AndroidNotificationScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) scheduler.rescheduleAll()
    }
}
