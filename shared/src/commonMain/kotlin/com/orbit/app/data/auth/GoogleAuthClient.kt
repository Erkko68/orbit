package com.orbit.app.data.auth

data class GoogleAuthCredential(
    val idToken: String,
    val accessToken: String? = null,
)

/**
 * Platform client that presents the Google account picker to the user and retrieves
 * the resulting credentials for Firebase Auth.
 */
interface GoogleAuthClient {
    /**
     * Prompts the user to select an account and returns their Google credential,
     * or null if the user cancelled the flow.
     */
    suspend fun getCredential(): GoogleAuthCredential?
}
