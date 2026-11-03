package com.orbit.app.feature.item

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** The usual moments to be reminded. */
private val ReminderPresets = listOf(Duration.ZERO, 10.minutes, 1.hours, 1.days)

/** Someone who can have the item, with their place in the space, which gives them a colour and a shape. */
data class ItemFace(val id: String, val name: String, val index: Int, val photoUrl: String?)

/**
 * Immutable state of the Item screen, the same for every item: its title, its description and the modules it has, or
 * a new item, which is not written until it has a name.
 */
data class ItemUiState(
    /** Opened from the add button, and not written yet. */
    val isNew: Boolean = false,
    val title: String = "",
    /** The title as typed, until it is saved. */
    val titleDraft: String? = null,
    val createdBy: String? = null,
    /** The sheet that picks when to remind is showing. */
    val editingReminder: Boolean = false,
    /** The moment being typed in that sheet, while its fields are open. */
    val customReminder: CustomDuration? = null,
    /** The sheet that confirms deleting the item is showing. */
    val confirmingDelete: Boolean = false,
    /** The item is gone, so the screen closes. */
    val deleted: Boolean = false,
    val description: String? = null,
    /** The description as typed, until it is saved. */
    val descriptionDraft: String? = null,
    val details: String? = null,
    /** The details as typed, until they are saved. */
    val detailsDraft: String? = null,
    val status: ItemStatus = ItemStatus.Open,
    val modules: List<Module> = emptyList(),
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val notFound: Boolean = false,
    val failed: Boolean = false,
    val submitting: Boolean = false,
    val spaceName: String? = null,
    /** The space's colour, null until it has loaded. */
    val accent: Int? = null,
    val memberNames: Map<String, String> = emptyMap(),
    val members: List<Member> = emptyList(),
    val currentUserId: String? = null,
    /** The schedule being edited, while its sheet is open. */
    val scheduleEditor: ScheduleEditor? = null,
    val checklistEditor: ChecklistEditor? = null,
    /** A reminder was just added, so notifications get allowed. */
    val askNotifications: Boolean = false,
    /** Whether the sheet that picks who has the item is open. */
    val pickingMember: Boolean = false,
    /** What is typed in the field that adds to the checklist. */
    val newEntry: String = "",
    /** Entries just ticked, by the state they had: they stay put a moment, so the tick is seen before the row moves. */
    val settling: Map<String, Boolean> = emptyMap(),
) {
    val isDone: Boolean get() = status is ItemStatus.Done

    /** A new item waits for its name, which is what gets it written: nothing else can be added before that. */
    val unnamed: Boolean get() = isNew && shownTitle.isBlank()

    /** There is an item to show. */
    val loaded: Boolean get() = !loading && !loadFailed && !notFound

    /** The screen can be used: a sheet can open, or a quick tap be written. */
    val canEdit: Boolean get() = loaded && !unnamed
    val canSubmit: Boolean get() = canEdit && !submitting
    val canSaveSchedule: Boolean get() = canSubmit && scheduleEditor?.schedule != null
    val canSaveChecklist: Boolean get() = canSubmit && checklistEditor?.canSave == true

    /** What the description field shows while it is typed, and after. */
    val shownDescription: String get() = descriptionDraft ?: description.orEmpty()

    val shownDetails: String get() = detailsDraft ?: details.orEmpty()

    val assignment: Assignment? get() = modules.filterIsInstance<Assignment>().firstOrNull()
    val schedule: Schedule? get() = modules.filterIsInstance<Schedule>().firstOrNull()
    val reminder: Reminder? get() = modules.filterIsInstance<Reminder>().firstOrNull()
    val reminderOffsets: List<Duration> get() = reminder?.offsets.orEmpty()

    /** The moments to pick from: the usual ones, and any other the reminder already has. */
    val reminderChoices: List<Duration> get() = (ReminderPresets + reminderOffsets).distinct().sorted()

    /** What the header shows while the title is being typed, and after. */
    val shownTitle: String get() = titleDraft ?: title

    /** The rule of `firestore.rules`: its creator, or someone who manages the space. Until it is written there is nothing to delete. */
    val canDelete: Boolean
        get() = !isNew && currentUserId != null && (
            createdBy == currentUserId || members.firstOrNull { it.userId == currentUserId }?.role?.managesSpace == true
        )

    val memberFaces: List<ItemFace>
        get() = members.mapIndexed { index, member -> ItemFace(member.userId, member.displayName, index, member.photoUrl) }

    /** Who has the item by name. */
    val assigneeIds: Set<String> get() = assignment?.assigneeIds.orEmpty()

    /** Who shows as having it: everyone when it is anyone's, nobody until it is someone's. */
    val assigneeFaces: List<ItemFace>
        get() = if (isAnyones) memberFaces else memberFaces.filter { it.id in assigneeIds }

    val isAnyones: Boolean get() = assignment == Assignment.Anyone

    private val checklist: Checklist? get() = modules.filterIsInstance<Checklist>().firstOrNull()
    private val entries: List<ChecklistEntry> get() = checklist?.entries.orEmpty()

    val hasChecklist: Boolean get() = checklist != null
    val hasEntries: Boolean get() = entries.isNotEmpty()

    /** One list, what is left first. An entry just ticked keeps its place for a moment. */
    val shownEntries: List<ChecklistEntry> get() = entries.sortedBy { settling[it.id] ?: it.checked }

    val checkedCount: Int get() = entries.count { it.checked }
    val entryCount: Int get() = entries.size
    val entryProgress: Float get() = if (entries.isEmpty()) 0f else checkedCount.toFloat() / entries.size

    val canAddEntry: Boolean get() = canEdit && hasChecklist && newEntry.isNotBlank()
}
