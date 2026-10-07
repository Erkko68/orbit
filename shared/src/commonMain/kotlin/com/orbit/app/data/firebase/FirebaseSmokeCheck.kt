package com.orbit.app.data.firebase

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

/**
 * Temporary (#6): proves anonymous sign-in and one Firestore write + read work from `commonMain`
 * on both platforms. Delete it when the real AuthRepository lands (#15).
 */
suspend fun firebaseSmokeCheck() {
    val log = Logger.withTag("FirebaseSmokeCheck")
    try {
        val auth = Firebase.auth
        val uid = (auth.currentUser ?: auth.signInAnonymously().user)?.uid ?: error("No user after sign-in")
        val document = Firebase.firestore.collection("users").document(uid)
        document.set(mapOf("language" to "en"), merge = true)
        log.i { "OK: uid=$uid, read back language=${document.get().get<String>("language")}" }
    } catch (e: Exception) {
        log.e(e) { "FAILED" }
    }
}
