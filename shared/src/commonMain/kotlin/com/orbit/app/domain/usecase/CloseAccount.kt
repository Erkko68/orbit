package com.orbit.app.domain.usecase

import com.orbit.app.domain.model.AuthError
import com.orbit.app.domain.model.AuthException
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.first

/**
 * Deletes the signed-in user's account and takes them out of every space (#188). Returns the names of the spaces
 * they own with other people in them, which have to be handed over first: then nothing changes at all.
 *
 * Otherwise the account is proven theirs with [password] (ignored for one that only signs in with Google) before
 * anything is deleted, so a wrong one leaves every space as it was. Then the spaces where they are alone go, they
 * leave the others, and the account goes last. Each step stands on its own, so a run that stops halfway can be
 * run again.
 */
suspend fun closeAccount(auth: AuthRepository, spaces: SpaceRepository, password: String): List<String> {
    val me = checkNotNull(auth.session.first()) { "Not signed in" }
    val mine = spaces.observeSpaces().first()
    val owned = mine.filter { space ->
        spaces.observeMembers(space.id).first().firstOrNull { it.userId == me.id }?.role == Role.OWNER
    }
    val shared = owned.filter { it.memberIds.size > 1 }
    if (shared.isNotEmpty()) return shared.map { it.name }
    if (me.hasPassword && password.isBlank()) throw AuthException(AuthError.WRONG_CREDENTIALS)
    auth.reauthenticate(password.takeIf { me.hasPassword })
    owned.forEach { spaces.deleteSpace(it.id) }
    (mine - owned.toSet()).forEach { spaces.leaveSpace(it.id) }
    auth.deleteAccount()
    return emptyList()
}
