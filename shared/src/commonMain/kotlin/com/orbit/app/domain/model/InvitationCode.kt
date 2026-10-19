package com.orbit.app.domain.model

import kotlin.random.Random
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

/** A code drawn from [random], for stand-ins and tests. The app derives its codes from a secure id instead. */
fun newInvitationCode(random: Random = Random): String =
    List(INVITATION_CODE_LENGTH) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

/** The link that opens an invitation in the app. A link for any browser needs a domain, which comes with #25. */
fun invitationLink(code: String): String = "orbit://join/$code"
