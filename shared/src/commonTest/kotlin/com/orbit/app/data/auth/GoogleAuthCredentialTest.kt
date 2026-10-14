package com.orbit.app.data.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GoogleAuthCredentialTest {
    @Test
    fun holdsTokens() {
        val credential = GoogleAuthCredential(idToken = "id-token", accessToken = "access-token")
        assertEquals("id-token", credential.idToken)
        assertEquals("access-token", credential.accessToken)

        val idOnly = GoogleAuthCredential(idToken = "id-token")
        assertEquals("id-token", idOnly.idToken)
        assertNull(idOnly.accessToken)
    }
}
