package com.orbit.app.data.auth

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the current foreground [Activity] through [Application.ActivityLifecycleCallbacks]
 * so platform clients (such as Credential Manager for Google Sign-In) can present UI in the
 * active activity's window.
 */
class ActivityProvider : Application.ActivityLifecycleCallbacks {
    private var currentActivityRef: WeakReference<Activity>? = null

    val currentActivity: Activity?
        get() = currentActivityRef?.get()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // Do not clear on pause: the Google Sign-in bottom sheet or credential picker
        // pauses the activity while presenting its UI. Clearing here would make
        // currentActivity null during credential retrieval.
    }

    override fun onActivityStopped(activity: Activity) {
        // Unused lifecycle callback.
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Unused lifecycle callback.
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
    }
}
