package com.orbit.app.domain.model

import kotlin.time.Duration.Companion.days

/** How long an invitation stays valid. The rules accept up to 30 days. */
val INVITATION_VALIDITY = 7.days

/** The code alphabet: capital letters without I and O, and the digits 2 to 9, so nothing looks like anything else. */
private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

const val INVITATION_CODE_LENGTH = 8

/** A code made of the first [INVITATION_CODE_LENGTH] characters of [randomId], from a secure generator. */
fun invitationCodeFrom(randomId: String): String {
    require(randomId.length >= INVITATION_CODE_LENGTH) { "Not enough random characters for a code" }
    return randomId.take(INVITATION_CODE_LENGTH).map { CODE_ALPHABET[it.code % CODE_ALPHABET.length] }.joinToString("")
}

/** Where `firebase/hosting` serves the page of an invitation link: Firebase Hosting on the app's project (#295). */
private const val INVITATION_HOST = "orbit-app-dev-ee2d7.web.app"

/**
 * The link a shared invitation carries. Chat and mail apps only make https addresses tappable, so it is a page that
 * shows the code and opens the app through `orbit://join/<code>`, which both apps still read (ADR 0002).
 */
fun invitationLink(code: String): String = "https://$INVITATION_HOST/join/$code"

/**
 * The code in [text], which is typed or pasted: the code itself or an invitation link of either form
 * (`https://…/join/<code>` or `orbit://join/<code>`), in any case, with spaces around, a closing slash, a query or a
 * fragment. Null when it is not shaped like a code.
 */
fun invitationCodeOf(text: String): String? {
    val code = text.trim().substringBefore('#').substringBefore('?').trimEnd('/').substringAfterLast('/').uppercase()
    return code.takeIf { it.length == INVITATION_CODE_LENGTH && it.all { char -> char in CODE_ALPHABET } }
}
