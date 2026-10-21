package com.orbit.app.core

import com.orbit.app.domain.model.invitationCodeOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The code of an invitation link the user opened, waiting for the spaces screen to offer joining it. The
 * link can arrive before the app is signed in, so it waits here until that screen is up.
 */
class PendingInvitation {
    private val pending = MutableStateFlow<String?>(null)

    val code: StateFlow<String?> = pending

    /** Keeps the code of [link] for the spaces screen. A link without a code is ignored. */
    fun offer(link: String) {
        invitationCodeOf(link)?.let { pending.value = it }
    }

    /** The spaces screen has taken the code. */
    fun consume() {
        pending.value = null
    }
}
