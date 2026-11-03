package com.orbit.app.feature.spacehome.items

import com.orbit.app.domain.model.Item
import com.orbit.app.feature.item.ItemFace
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.filter_all
import orbit.shared.generated.resources.filter_lists
import orbit.shared.generated.resources.filter_mine
import org.jetbrains.compose.resources.StringResource

data class SpaceItemsUiState(
    val filter: ItemFilter = ItemFilter.ALL,
    /** The items have not arrived yet. */
    val loading: Boolean = false,
    /** Loading the items failed. Cleared by a retry. */
    val loadFailed: Boolean = false,
    /** The space's items that pass [filter], open ones first, without the one in [turn]. */
    val items: List<SpaceItem> = emptyList(),
    /** What is the user's to do next. Stays here, done, right after they finish it, so they can undo it. */
    val turn: SpaceItem? = null,
    /** The signed-in user, for the turn card. */
    val userInitials: String = "",
    val userPhotoUrl: String? = null,
    val today: LocalDate? = null,
    /** Completing or reopening an item failed. Cleared by the next attempt. */
    val failed: Boolean = false,
    /** A complete or reopen write is currently in progress. */
    val submitting: Boolean = false,
) {
    /** What has a checklist still open shows how far along it is, as a tile. */
    val lists: List<SpaceItem> get() = items.filter { it.checklist != null && !it.done }

    /** Everything else, as rows. */
    val rows: List<SpaceItem> get() = items - lists.toSet()
    val openRows: Int get() = rows.count { !it.done }

    val isEmpty: Boolean get() = items.isEmpty() && turn == null
}

data class SpaceItem(
    val id: String,
    val title: String,
    val done: Boolean = false,
    /** Its short description. */
    val summary: String? = null,
    /** Who has it: its assignees, or everyone when it is anyone's. */
    val faces: List<ItemFace> = emptyList(),
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val checklist: SpaceItemChecklist? = null,
)

data class SpaceItemChecklist(val done: Int, val total: Int, val peek: SpaceItemPeek = SpaceItemPeek()) {
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
}

/** A look at what a list still needs: its first few [entries], and how many [more] there are. */
data class SpaceItemPeek(val entries: List<String> = emptyList(), val more: Int = 0)

/** The filters over the items. */
enum class ItemFilter(val label: StringResource, val matches: (Item, String?) -> Boolean) {
    ALL(Res.string.filter_all, { _, _ -> true }),
    MINE(Res.string.filter_mine, { item, userId -> userId != null && item.modules.isAssignedTo(userId) }),
    LISTS(Res.string.filter_lists, { item, _ -> item.modules.checklist != null }),
}
