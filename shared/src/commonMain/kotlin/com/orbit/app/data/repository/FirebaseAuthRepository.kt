package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.auth.GoogleAuthClient
import com.orbit.app.data.mapper.devicePreferencesDocument
import com.orbit.app.data.mapper.googleSignInException
import com.orbit.app.data.mapper.mapUser
import com.orbit.app.data.mapper.registerException
import com.orbit.app.data.mapper.signInException
import com.orbit.app.data.mapper.userProfileDocument
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val googleAuthClient: GoogleAuthClient,
) : AuthRepository {
    /** Lives as long as the app: this repository is a singleton. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Counts the links made here: the auth state does not change with one, so [session] maps the user again. */
    private val linked = MutableStateFlow(0)

    /**
     * The signed-in user from their `users/{uid}` document, with the Firebase account filling what it lacks.
     * Every collector shares one listener, which stops a few seconds after the last one leaves.
     */
    override val session: Flow<User?> = combine(auth.authStateChanged, linked) { user, _ -> user }.flatMapLatest { firebaseUser ->
        if (firebaseUser == null) {
            flowOf(null)
        } else {
            firestore.collection(USERS).document(firebaseUser.uid).snapshots
                .map { firebaseUser.toUser(it) }
                .catch { e ->
                    Logger.w(e) { "Failed to read user document for ${firebaseUser.uid}" }
                    emit(firebaseUser.toUser(null))
                }
        }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), replay = 1)

    override suspend fun signIn(email: String, password: String) {
        val result = try {
            auth.signInWithEmailAndPassword(email, password)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw signInException(e)
        }
        val user = result.user ?: auth.currentUser
        if (user != null) {
            updateDevicePreferences(user.uid)
        }
    }

    override suspend fun register(email: String, password: String, displayName: String) {
        val result = try {
            auth.createUserWithEmailAndPassword(email, password)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw registerException(e)
        }
        val user = checkNotNull(result.user) { "No user returned after registration" }
        val profile = userProfileDocument(displayName = displayName) + mapOf("createdAt" to FieldValue.serverTimestamp)
        firestore.collection(USERS)
            .document(user.uid)
            .set(profile, merge = true)
    }

    /**
     * Renames the user in their `users/{uid}` profile and in their member document in every space
     * they belong to in a single batch, so profile copies never drift (ADR 0002).
     */
    override suspend fun updateDisplayName(displayName: String) {
        val trimmed = displayName.trim()
        require(trimmed.isNotBlank()) { "A display name is required" }
        val user = signedInUser()

        val spacesSnapshot = firestore.collection(SPACES).where { "memberIds" contains user.uid }.get()

        val batch = firestore.batch()
        val userDoc = firestore.collection(USERS).document(user.uid)
        batch.set(userDoc, mapOf("displayName" to trimmed), merge = true)

        spacesSnapshot.documents.forEach { spaceDoc ->
            val memberDoc = firestore.collection(SPACES)
                .document(spaceDoc.id)
                .collection(MEMBERS)
                .document(user.uid)
            batch.set(memberDoc, mapOf("displayName" to trimmed), merge = true)
        }

        writeLocally("Rename user ${user.uid}") { batch.commit() }
    }

    /**
     * Firebase only changes a password for a session it has just seen the old one for, so this proves the
     * current one first. A wrong one and a weak new one both come back as refused credentials, which
     * [signInException] tells apart (#188).
     */
    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = signedInUser()
        reauthenticate(user, currentPassword)
        try {
            user.updatePassword(newPassword)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw signInException(e)
        }
    }

    /** Does nothing when the user closes the account picker. */
    override suspend fun linkGoogle() {
        val google = googleAuthClient.getCredential() ?: return
        link(GoogleAuthProvider.credential(google.idToken, google.accessToken)) { googleSignInException(it) }
    }

    override suspend fun setPassword(newPassword: String) {
        val email = checkNotNull(signedInUser().email) { "The account has no email to set a password for" }
        link(EmailAuthProvider.credential(email, newPassword)) { registerException(it) }
    }

    /** Adds [credential] as another way into the signed-in account, which keeps its `uid` (#293). */
    private suspend fun link(credential: AuthCredential, failure: (Exception) -> Exception) {
        try {
            signedInUser().linkWithCredential(credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw failure(e)
        }
        linked.update { it + 1 }
    }

    override suspend fun reauthenticate(currentPassword: String?) {
        val user = signedInUser()
        if (currentPassword != null) reauthenticate(user, currentPassword) else reauthenticateWithGoogle(user)
    }

    /**
     * Deletes `users/{uid}` and then the Firebase account (#188), in that order: once the account is gone its
     * profile can no longer be written, and a run that stops halfway can simply be run again.
     */
    override suspend fun deleteAccount() {
        val user = signedInUser()
        firestore.collection(USERS).document(user.uid).delete()
        try {
            user.delete()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw signInException(e)
        }
    }

    /** Proves the password of the account being acted on. A wrong one is [AuthError.WRONG_CREDENTIALS]. */
    private suspend fun reauthenticate(user: FirebaseUser, password: String) {
        val email = checkNotNull(user.email) { "The account has no email to sign in with" }
        try {
            user.reauthenticate(EmailAuthProvider.credential(email, password))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw signInException(e)
        }
    }

    /** The same for an account that signs in with Google: closing the picker leaves the account alone. */
    private suspend fun reauthenticateWithGoogle(user: FirebaseUser) {
        val google = googleAuthClient.getCredential() ?: throw AuthException(AuthError.WRONG_CREDENTIALS)
        try {
            user.reauthenticate(GoogleAuthProvider.credential(google.idToken, google.accessToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw googleSignInException(e)
        }
    }

    /** Does nothing when the user closes the account picker. */
    override suspend fun signInWithGoogle() {
        val google = googleAuthClient.getCredential() ?: return
        val result = try {
            auth.signInWithCredential(GoogleAuthProvider.credential(google.idToken, google.accessToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw googleSignInException(e)
        }
        ensureUserProfile(checkNotNull(result.user) { "No user returned after Google sign-in" })
    }

    /**
     * Gives a first-time Google user a profile, or updates language and time zone from the device
     * if the profile already exists. Only when the read says there is none: a failed read writes
     * nothing, so an existing profile is never overwritten with the Google one.
     */
    private suspend fun ensureUserProfile(user: FirebaseUser) {
        val document = firestore.collection(USERS).document(user.uid)
        try {
            if (document.get().exists) {
                updateDevicePreferences(user.uid)
                return
            }
            val displayName = user.displayName?.ifBlank { null } ?: user.email?.substringBefore('@').orEmpty()
            val profile = userProfileDocument(displayName = displayName, photoUrl = user.photoURL) +
                mapOf("createdAt" to FieldValue.serverTimestamp)
            document.set(profile, merge = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The session shows the Google name and photo meanwhile; the next sign-in tries again.
            Logger.w(e) { "Could not create or update the profile of ${user.uid}" }
        }
    }

    /** Syncs the device's language and time zone to the user's private profile document. */
    private suspend fun updateDevicePreferences(uid: String) {
        try {
            firestore.collection(USERS)
                .document(uid)
                .set(devicePreferencesDocument(), merge = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Could not update device preferences for $uid" }
        }
    }

    /**
     * Firebase refuses an address it has no account for, which would tell a stranger who is registered. That
     * answer is swallowed: the caller cannot tell it from a link that was sent, which is the point (#217).
     */
    override suspend fun sendPasswordReset(email: String) {
        try {
            auth.sendPasswordResetEmail(email.trim())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val failure = signInException(e)
            if (failure.error == AuthError.WRONG_CREDENTIALS) {
                Logger.d { "A password reset was asked for an address with no account" }
                return
            }
            throw failure
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun idToken(): String {
        val user = signedInUser()
        return checkNotNull(user.getIdToken(false)) { "No ID token for ${user.uid}" }
    }

    override suspend fun acceptAiConsent() {
        val user = signedInUser()
        writeLocally("Record the AI consent of ${user.uid}") {
            firestore.collection(USERS).document(user.uid)
                .set(mapOf("aiConsentAt" to FieldValue.serverTimestamp), merge = true)
        }
    }

    /** The signed-in Firebase account, for a write or a token made as them. Throws when signed out. */
    private fun signedInUser(): FirebaseUser = checkNotNull(auth.currentUser) { "Not signed in" }

    /** The user from their profile document, or from the Firebase account alone when [profile] is null. */
    private fun FirebaseUser.toUser(profile: DocumentSnapshot?) = mapUser(
        id = uid,
        displayName = profile?.field("displayName"),
        photoUrl = profile?.field("photoUrl"),
        fallbackDisplayName = displayName ?: email?.substringBefore('@'),
        fallbackPhotoUrl = photoURL,
        email = email,
        aiConsent = profile?.instant("aiConsentAt") != null,
        hasPassword = providerData.any { it.providerId == PASSWORD_PROVIDER },
        signsInWithGoogle = providerData.any { it.providerId == GOOGLE_PROVIDER },
    )
}

/** Firebase's names for the email and password provider and for Google, as `providerData` gives them. */
private const val PASSWORD_PROVIDER = "password"
private const val GOOGLE_PROVIDER = "google.com"
