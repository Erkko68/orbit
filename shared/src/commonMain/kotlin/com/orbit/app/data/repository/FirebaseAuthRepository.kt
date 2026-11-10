package com.orbit.app.data.repository

import co.touchlab.kermit.Logger
import com.orbit.app.data.auth.GoogleAuthClient
import com.orbit.app.data.auth.GoogleAuthCredential
import com.orbit.app.data.auth.googleIdentity
import com.orbit.app.data.mapper.devicePreferencesDocument
import com.orbit.app.data.mapper.googleSignInException
import com.orbit.app.data.mapper.mapUser
import com.orbit.app.data.mapper.registerException
import com.orbit.app.data.mapper.signInException
import com.orbit.app.data.mapper.userProfileDocument
import com.orbit.app.domain.model.AccountLinkException
import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.LinkProof
import com.orbit.app.domain.model.User
import com.orbit.app.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val googleAuthClient: GoogleAuthClient,
) : AuthRepository {
    /** Lives as long as the app: this repository is a singleton. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The Google credential of a sign-in that collided with a password account, kept so linking it after
     * the password is typed does not open the account picker a second time (#173).
     */
    private var pendingGoogle: GoogleAuthCredential? = null

    /**
     * The signed-in user from their `users/{uid}` document, with the Firebase account filling what it lacks.
     * Every collector shares one listener, which stops a few seconds after the last one leaves.
     */
    override val session: Flow<User?> = auth.authStateChanged.flatMapLatest { firebaseUser ->
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
            val failure = signInException(e)
            // The account may be a Google one without a password: an offer to link, not a wrong password (#173).
            if (failure.error == AuthError.WRONG_CREDENTIALS && signsInWithGoogleOnly(email)) {
                throw AccountLinkException(email, LinkProof.GOOGLE, e)
            }
            throw failure
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

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        Logger.w { "changePassword is a stub (#188)" }
    }

    override suspend fun deleteAccount() {
        Logger.w { "deleteAccount is a stub (#188)" }
    }

    /** Does nothing when the user closes the account picker. */
    override suspend fun signInWithGoogle() {
        val google = googleAuthClient.getCredential() ?: return
        val result = try {
            auth.signInWithCredential(GoogleAuthProvider.credential(google.idToken, google.accessToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw googleCollision(google, e)
        }
        pendingGoogle = null
        ensureUserProfile(checkNotNull(result.user) { "No user returned after Google sign-in" })
    }

    /**
     * Signs in with the password, which proves the account is the user's, and links the Google credential
     * that collided with it to that same account, so its `uid`, profile and spaces are the ones they keep.
     */
    override suspend fun linkGoogleWithPassword(email: String, password: String) {
        val google = pendingGoogle ?: googleAuthClient.getCredential() ?: return
        requireGoogleAddress(google, email)
        signIn(email, password)
        linkCredential(GoogleAuthProvider.credential(google.idToken, google.accessToken)) { googleSignInException(it) }
        pendingGoogle = null
        ensureUserProfile(signedInUser())
    }

    /** Signing in with Google proves the account is the user's; the password is then linked to it. */
    override suspend fun setPasswordWithGoogle(email: String, password: String) {
        val google = googleAuthClient.getCredential() ?: return
        requireGoogleAddress(google, email)
        val result = try {
            auth.signInWithCredential(GoogleAuthProvider.credential(google.idToken, google.accessToken))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw googleCollision(google, e)
        }
        linkCredential(EmailAuthProvider.credential(email.trim(), password)) { registerException(it) }
        ensureUserProfile(checkNotNull(result.user) { "No user returned after Google sign-in" })
    }

    /**
     * What a refused Google sign-in means: an address that already has a password account is an offer to
     * link it, with the credential kept for [linkGoogleWithPassword]. A token that proves no address
     * cannot be linked to anything, so that stays the plain failure.
     */
    private fun googleCollision(google: GoogleAuthCredential, cause: Exception): Exception {
        if (cause is FirebaseAuthUserCollisionException) {
            val identity = googleIdentity(google.idToken)
            if (identity != null && identity.emailVerified) {
                pendingGoogle = google
                return AccountLinkException(identity.email, LinkProof.PASSWORD, cause)
            }
            Logger.w(cause) { "A Google sign-in collided with an account, but its token proves no address" }
        }
        return googleSignInException(cause)
    }

    /** Refuses a Google account that is not the address being linked, so another user's account is never joined. */
    private fun requireGoogleAddress(google: GoogleAuthCredential, email: String) {
        val identity = googleIdentity(google.idToken) ?: return
        if (!identity.email.equals(email.trim(), ignoreCase = true)) {
            throw AuthException(AuthError.DIFFERENT_GOOGLE_ACCOUNT)
        }
    }

    /**
     * Adds [credential] as another way into the signed-in account. A provider the account already has is
     * no failure: pressing the button twice leaves the one account linked.
     */
    private suspend fun linkCredential(credential: AuthCredential, failure: (Throwable) -> Exception) {
        val user = signedInUser()
        try {
            user.linkWithCredential(credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is FirebaseAuthUserCollisionException &&
                user.providerData.any { it.providerId == credential.providerId }
            ) {
                Logger.d { "${credential.providerId} is already linked to ${user.uid}" }
                return
            }
            throw failure(e)
        }
    }

    /**
     * Whether [email] signs in with Google and has no password. Firebase's answer is a hint, not a secret:
     * a failed or empty answer (its email enumeration protection) leaves the sign-in a wrong password.
     */
    @Suppress("DEPRECATION")
    private suspend fun signsInWithGoogleOnly(email: String): Boolean = try {
        val methods = auth.fetchSignInMethodsForEmail(email)
        GOOGLE_PROVIDER in methods && PASSWORD_PROVIDER !in methods
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.d(e) { "Could not read the sign-in methods of an address" }
        false
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
    )
}

/** Firebase's name for the Google provider, as `fetchSignInMethodsForEmail` and `providerData` give it. */
private const val GOOGLE_PROVIDER = "google.com"

/** Firebase's name for the email and password provider. */
private const val PASSWORD_PROVIDER = "password"
