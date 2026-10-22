package com.orbit.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.orbit.app.core.PendingInvitation
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val pendingInvitation: PendingInvitation by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The link that started the app. After a rotation the same intent comes back: it was handled already.
        if (savedInstanceState == null) offerInvitation(intent)

        setContent {
            App()
        }
    }

    /** The app is open already and a link is tapped: this activity is reused ("singleTop"), not started again. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        offerInvitation(intent)
    }

    private fun offerInvitation(intent: Intent?) {
        intent?.data?.let { pendingInvitation.offer(it.toString()) }
    }
}
