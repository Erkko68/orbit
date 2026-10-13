package com.orbit.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthValidationTest {
    @Test
    fun aValidSignInHasNoErrors() {
        assertEquals(emptyList(), validateSignIn("ana@orbit.app", "x"))
        assertEquals(emptyList(), validateSignIn("  ana@orbit.app  ", "short"))
    }

    @Test
    fun signInNeedsAnEmailAndAPassword() {
        assertEquals(
            listOf(CredentialsError.BlankEmail, CredentialsError.BlankPassword),
            validateSignIn("   ", ""),
        )
    }

    @Test
    fun aValidRegistrationHasNoErrors() {
        assertEquals(emptyList(), validateRegistration("ana@orbit.app", "12345678", "12345678"))
    }

    @Test
    fun theEmailMustLookLikeOne() {
        val invalid = listOf(
            "ana", "ana@", "@orbit.app", "ana@orbit", "ana@.app", "ana@orbit.", "a@b@orbit.app", "a na@orbit.app",
        )

        for (email in invalid) {
            assertEquals(listOf(CredentialsError.InvalidEmail), validateSignIn(email, "x"), email)
        }
    }

    @Test
    fun thePasswordMustHaveAtLeastEightCharacters() {
        assertEquals(
            listOf(CredentialsError.ShortPassword),
            validateRegistration("ana@orbit.app", "1234567", "1234567"),
        )
        assertEquals(emptyList(), validateRegistration("ana@orbit.app", "12345678", "12345678"))
    }

    @Test
    fun theConfirmationMustMatchThePassword() {
        assertEquals(
            listOf(CredentialsError.PasswordsDiffer),
            validateRegistration("ana@orbit.app", "12345678", "12345679"),
        )
    }

    @Test
    fun registrationReportsEveryProblemAtOnce() {
        assertEquals(
            listOf(CredentialsError.BlankEmail, CredentialsError.ShortPassword, CredentialsError.PasswordsDiffer),
            validateRegistration("", "123", "456"),
        )
    }
}
