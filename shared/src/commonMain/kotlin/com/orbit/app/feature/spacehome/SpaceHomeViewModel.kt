package com.orbit.app.feature.spacehome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.role_admin
import orbit.shared.generated.resources.role_member
import orbit.shared.generated.resources.role_owner

class SpaceHomeViewModel(spaceId: String, spaces: SpaceRepository, items: ItemRepository) : ViewModel() {
    private val filter = MutableStateFlow(ItemFilter.ALL)

    val uiState: StateFlow<SpaceHomeUiState> = combine(
        spaces.observeSpaces(),
        spaces.observeMembers(spaceId),
        items.observeItems(spaceId),
        filter,
    ) { allSpaces, members, spaceItems, selected ->
        SpaceHomeUiState(
            name = allSpaces.firstOrNull { it.id == spaceId }?.name.orEmpty(),
            members = members.map(::toSpaceMember),
            filter = selected,
            items = spaceItems.filter { selected.matches(it.preset) }.map(::toSpaceItem),
            openTasks = spaceItems.count { it.status == ItemStatus.Open && it.preset in TASK_PRESETS },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpaceHomeUiState())

    fun select(filter: ItemFilter) {
        this.filter.value = filter
    }
}

/** Items without a preset count as tasks too. */
private val TASK_PRESETS = setOf(Preset.TASK, null)

private fun toSpaceMember(member: Member) = SpaceMember(
    id = member.userId,
    name = member.displayName,
    initials = initialsOf(member.displayName),
    role = when (member.role) {
        Role.OWNER -> Res.string.role_owner
        Role.ADMIN -> Res.string.role_admin
        Role.MEMBER -> Res.string.role_member
    },
)

private fun toSpaceItem(item: Item) = SpaceItem(
    id = item.id,
    title = item.title,
    type = when (item.preset) {
        Preset.TASK -> ItemType.TASK
        Preset.SHOPPING_LIST -> ItemType.LIST
        Preset.EVENT -> ItemType.EVENT
        null -> ItemType.OTHER
    },
)

/** Up to two initials, from the first letters of the name's words: "Ana López" is "AL". */
internal fun initialsOf(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
