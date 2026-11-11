package com.orbit.app.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The tokens are real base64url payloads, signed by nobody: only the claims are read (#173). */
class GoogleIdTokenTest {
    private fun token(payload: String) = "$HEADER.$payload.signature-is-not-read"

    @Test
    fun readsTheVerifiedAddress() {
        val identity = googleIdentity(token("eyJlbWFpbCI6ImFuYUBvcmJpdC5hcHAiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZX0"))

        assertEquals("ana@orbit.app", identity?.email)
        assertTrue(identity?.emailVerified == true)
    }

    @Test
    fun readsAVerifiedFlagSentAsText() {
        val identity = googleIdentity(token("eyJlbWFpbCI6ImFuYUBvcmJpdC5hcHAiLCJlbWFpbF92ZXJpZmllZCI6InRydWUifQ"))

        assertEquals("ana@orbit.app", identity?.email)
        assertTrue(identity?.emailVerified == true)
    }

    @Test
    fun anUnverifiedAddressProvesNothing() {
        val identity = googleIdentity(token("eyJlbWFpbCI6ImFuYUBvcmJpdC5hcHAiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2V9"))

        assertEquals("ana@orbit.app", identity?.email)
        assertFalse(identity?.emailVerified == true)
    }

    @Test
    fun aTokenWithoutAnAddressIsNoIdentity() {
        assertNull(googleIdentity(token("eyJzdWIiOiIxMjM0In0")))
        assertNull(googleIdentity(token("eyJlbWFpbCI6IiAgIiwiZW1haWxfdmVyaWZpZWQiOnRydWV9")))
    }

    @Test
    fun whatIsNotATokenIsNoIdentity() {
        assertNull(googleIdentity(""))
        assertNull(googleIdentity("one-part-only"))
        assertNull(googleIdentity(token("not base64url at all!")))
        assertNull(googleIdentity(token("eyJlbWFpbCI")))
    }

    private companion object {
        const val HEADER = "eyJhbGciOiJSUzI1NiJ9"
    }
}
