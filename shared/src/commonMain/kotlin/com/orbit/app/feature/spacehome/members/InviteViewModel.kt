package com.orbit.app.feature.spacehome.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Invitation
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.InvitationRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    /** Members who cannot be read cannot be checked, so nobody can invite until they load. */
    private val canInvite: Flow<Boolean> = combine(
        auth.session,
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
    ) { user, members ->
        members.firstOrNull { it.userId == user?.id }?.role?.managesSpace == true
    }.distinctUntilChanged()

    /**
     * The newest invitation that still works, or [Load.Loading] until the invitations arrive: the card
     * shows as soon as the user is known to be able to invite and does not wait for them. Members who cannot
     * invite never ask for them. Invitations that fail to load show as none, so a new one can still be made.
     */
    private val activeInvitation: Flow<Load<Invitation?>> = canInvite.flatMapLatest { can ->
        if (can) {
            invitations.observeInvitations(spaceId)
                .orOnFailure("Loading the invitations of $spaceId", emptyList())
                .map<List<Invitation>, Load<Invitation?>> { all ->
                    Load.Loaded(all.firstOrNull { it.isActive(Clock.System.now()) })
                }
                .onStart { emit(Load.Loading) }
        } else {
            flowOf(Load.Loaded(null))
        }
    }

    /** Only the share message needs the name, so the card does not wait for it. */
    private val spaceName: Flow<String> = spaces.observeSpaces()
        .orOnFailure("Loading the spaces", emptyList())
        .map { all -> all.firstOrNull { it.id == spaceId }?.name.orEmpty() }
        .onStart { emit("") }

    val uiState: StateFlow<InviteUiState> =
        combine(canInvite, activeInvitation, spaceName, form) { can, load, name, form ->
            form.copy(
                canInvite = can,
                spaceName = name,
                loading = load == Load.Loading,
                invitation = load.valueOrNull?.let { ActiveInvitation(it.code, it.link) },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InviteUiState())

    fun createInvitation() = submit(InviteFailure.CREATE) { invitations.createInvitation(spaceId) }

    fun revokeInvitation() {
        val code = uiState.value.invitation?.code ?: return
        submit(InviteFailure.REVOKE) { invitations.revokeInvitation(code) }
    }

    private fun submit(onFailure: InviteFailure, action: suspend () -> Unit) {
        if (form.value.busy) return
        form.update { it.copy(busy = true, failure = null) }
        viewModelScope.launch {
            val failed = attempt("Invitation action $onFailure") { action() }.isFailure
            form.update { it.copy(busy = false, failure = onFailure.takeIf { failed }) }
        }
    }
}
