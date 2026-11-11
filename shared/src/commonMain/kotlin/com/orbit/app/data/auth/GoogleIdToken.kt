package com.orbit.app.data.auth

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Who a Google ID token is for, as the token itself says. */
internal data class GoogleIdentity(val email: String, val emailVerified: Boolean)

/**
 * The address inside a Google ID token, read from its payload without checking the signature: it only
 * says which account collided, and Firebase is what trusts or refuses the token (#173). Null when the
 * token carries no address.
 */
internal fun googleIdentity(idToken: String): GoogleIdentity? {
    val payload = idToken.split('.').getOrNull(1)?.let(::decodeBase64Url)?.decodeToString() ?: return null
    val claims = runCatching { Json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: return null
    val email = runCatching { claims["email"]?.jsonPrimitive?.content }.getOrNull()?.ifBlank { null } ?: return null
    val verified = runCatching { claims["email_verified"]?.jsonPrimitive?.content }.getOrNull()
    return GoogleIdentity(email = email, emailVerified = verified?.lowercase() == "true")
}

private const val BASE64_URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

/** The bytes of one base64url section of a token, which carries no padding. Null when it is not base64url. */
private fun decodeBase64Url(section: String): ByteArray? {
    val bytes = ArrayList<Byte>(section.length * 3 / 4 + 1)
    var buffer = 0
    var bits = 0
    for (character in section) {
        if (character == '=') break
        val value = BASE64_URL.indexOf(character)
        if (value < 0) return null
        buffer = (buffer shl 6) or value
        bits += 6
        if (bits >= 8) {
            bits -= 8
            bytes.add(((buffer shr bits) and 0xFF).toByte())
        }
    }
    return bytes.toByteArray()
}
