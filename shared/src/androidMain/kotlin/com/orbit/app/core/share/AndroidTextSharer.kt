package com.orbit.app.core.share

import android.content.Context
import android.content.Intent

/** Shares through the Android chooser. Started from the application context, so it opens in a new task. */
class AndroidTextSharer(private val context: Context) : TextSharer {
    override fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
