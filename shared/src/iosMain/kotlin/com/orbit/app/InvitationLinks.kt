package com.orbit.app

import com.orbit.app.core.PendingInvitation
import org.koin.mp.KoinPlatform

/** Called from Swift with a link the user opened, such as `orbit://join/<code>`. */
fun offerInvitationLink(link: String) {
    KoinPlatform.getKoin().get<PendingInvitation>().offer(link)
}
