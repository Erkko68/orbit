package com.orbit.app.feature.spacehome.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.orbit.app.domain.model.Invitation
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** The invitation card of the members tab: an owner or admin creates, shares and revokes the link of a space. */
@OptIn(ExperimentalCoroutinesApi::class)
class InviteViewModel(
    private val spaceId: String,
    private val invitations: InvitationRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    /** Busy and failure: what the card is doing, not what the repositories hold. */
    private val form = MutableStateFlow(InviteUiState())

    private val canInvite: Flow<Boolean> = combine(auth.session, spaces.observeMembers(spaceId)) { user, members ->
        members.firstOrNull { it.userId == user?.id }?.role in INVITING_ROLES
    }.distinctUntilChanged()

    /**
     * The newest invitation that still works, or [InvitationLoad.Loading] until the invitations arrive: the card
     * shows as soon as the user is known to be able to invite and does not wait for them. Members who cannot
     * invite never ask for them. A repository that gives up on them ends without a word, which shows as none.
     */
    private val activeInvitation: Flow<InvitationLoad> = canInvite.flatMapLatest { can ->
        if (can) {
            invitations.observeInvitations(spaceId)
                .catch { cause ->
                    Logger.e(cause) { "Loading the invitations failed" }
                    emit(emptyList())
                }
                .onEmpty { emit(emptyList()) }
                .map<List<Invitation>, InvitationLoad> { all ->
                    InvitationLoad.Loaded(all.firstOrNull { it.isActive(Clock.System.now()) })
                }
                .onStart { emit(InvitationLoad.Loading) }
        } else {
            flowOf(InvitationLoad.Loaded(null))
        }
    }

    /** Only the share message needs the name, so the card does not wait for it. */
    private val spaceName: Flow<String> = spaces.observeSpaces()
        .map { all -> all.firstOrNull { it.id == spaceId }?.name.orEmpty() }
        .onStart { emit("") }

    val uiState: StateFlow<InviteUiState> =
        combine(canInvite, activeInvitation, spaceName, form) { can, load, name, form ->
            form.copy(
                canInvite = can,
                spaceName = name,
                loading = load == InvitationLoad.Loading,
                invitation = (load as? InvitationLoad.Loaded)?.invitation?.let { ActiveInvitation(it.code, it.link) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InviteUiState())

    fun createInvitation() = attempt(InviteFailure.CREATE) { invitations.createInvitation(spaceId) }

    fun revokeInvitation() {
        val code = uiState.value.invitation?.code ?: return
        attempt(InviteFailure.REVOKE) { invitations.revokeInvitation(code) }
    }

    private fun attempt(onFailure: InviteFailure, action: suspend () -> Unit) {
        if (form.value.busy) return
        form.update { it.copy(busy = true, failure = null) }
        viewModelScope.launch {
            try {
                action()
                form.update { it.copy(busy = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(e) { "Invitation action failed: $onFailure" }
                form.update { it.copy(busy = false, failure = onFailure) }
            }
        }
    }

    private companion object {
        val INVITING_ROLES = setOf(Role.OWNER, Role.ADMIN)
    }
}

/** What is known of the space's invitations: nothing yet, or the one that works, if any. */
private sealed interface InvitationLoad {
    data object Loading : InvitationLoad

    data class Loaded(val invitation: Invitation?) : InvitationLoad
}
