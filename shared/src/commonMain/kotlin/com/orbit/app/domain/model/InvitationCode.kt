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

/** The link that opens an invitation in the app. A link for any browser needs a domain the app does not have yet. */
fun invitationLink(code: String): String = "orbit://join/$code"

/**
 * The code in [text], which is typed or pasted: the code itself or an invitation link, in any case and with
 * spaces around. Null when it is not shaped like a code.
 */
fun invitationCodeOf(text: String): String? {
    val code = text.trim().substringBefore('?').substringAfterLast('/').uppercase()
    return code.takeIf { it.length == INVITATION_CODE_LENGTH && it.all { char -> char in CODE_ALPHABET } }
}
