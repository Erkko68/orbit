package com.orbit.app.core.share

/** Hands some text to the system share sheet, which lets the user pick the app to send it with. */
fun interface TextSharer {
    fun share(text: String)
}
