package com.orbit.app.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ITEM_TITLE_MAX_LENGTH
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** How long a ticked entry stays where it is, so the tick is seen before the row moves. */
private val ENTRY_SETTLE = 450.milliseconds

/** One item, or with an empty [itemId] a new one, which is written the first time something is saved on it. */
class ItemViewModel(
    private val spaceId: String,
    itemId: String,
    private val items: ItemRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val startsNew = itemId.isEmpty()
    private val id = MutableStateFlow(itemId.ifEmpty { null })
    private val creating = Mutex()

    /** What the user is doing on the screen. The rest stays empty: [uiState] fills it from the repositories. */
    private val form = MutableStateFlow(ItemUiState())
    private val retries = Channel<Unit>(Channel.CONFLATED)

    private val item = combine(items.observeItems(spaceId), id) { all, id -> all.firstOrNull { it.id == id } }
        .asLoad("Loading item $itemId in space $spaceId", retries)

    /** Only decorates the item, so a failed read leaves no space name and unknown members. */
    private val context = combine(
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        auth.session,
    ) { all, members, user ->
        val space = all.firstOrNull { it.id == spaceId }
        ItemContext(
            spaceName = space?.name,
            accent = space?.accent,
            memberNames = members.associate { it.userId to it.displayName },
            members = members,
            currentUserId = user?.id,
        )
    }.onStart { emit(ItemContext()) }

    val uiState: StateFlow<ItemUiState> = combine(item, context, form) { load, context, form ->
        val loaded = load.valueOrNull
        form.copy(
            isNew = startsNew && loaded == null,
            title = loaded?.title.orEmpty(),
            createdBy = loaded?.createdBy,
            description = loaded?.description,
            details = loaded?.details,
            status = loaded?.status ?: form.status,
            modules = loaded?.modules?.all.orEmpty(),
            loading = !startsNew && load == Load.Loading,
            loadFailed = !startsNew && load == Load.Failed,
            notFound = !startsNew && load is Load.Loaded && loaded == null,
            spaceName = context.spaceName,
            accent = context.accent,
            memberNames = context.memberNames,
            members = context.members,
            currentUserId = context.currentUserId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemUiState(isNew = startsNew, loading = !startsNew))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    fun complete() = submit { itemId -> items.complete(spaceId, itemId) }

    fun reopen() = submit { itemId -> items.reopen(spaceId, itemId) }

    fun onTitleChange(text: String) = form.update { it.copy(titleDraft = text, failed = false) }

    fun onDescriptionChange(text: String) = form.update { it.copy(descriptionDraft = text, failed = false) }

    fun onDetailsChange(text: String) = form.update { it.copy(detailsDraft = text, failed = false) }

    /**
     * Saves what was typed, in one go whichever field the user left. A blank title goes back to the one it had, and a
     * failure keeps the text. A new item is written here with all of it, once it has a name.
     */
    fun saveDrafts() {
        val current = uiState.value
        val title = form.value.titleDraft?.trim()?.take(ITEM_TITLE_MAX_LENGTH)
        val description = form.value.descriptionDraft?.trim()
        val details = form.value.detailsDraft?.trim()
        val renames = !title.isNullOrEmpty() && title != current.title
        val describes = description != null && description != current.description.orEmpty()
        val explains = details != null && details != current.details.orEmpty()
        // Only what was saved: the user may already be typing in the other field.
        val forgetSaved = {
            form.update {
                it.copy(
                    titleDraft = it.titleDraft.takeUnless { draft -> draft?.trim()?.take(ITEM_TITLE_MAX_LENGTH) == title },
                    descriptionDraft = it.descriptionDraft.takeUnless { draft -> draft?.trim() == description },
                    detailsDraft = it.detailsDraft.takeUnless { draft -> draft?.trim() == details },
                )
            }
        }
        if (!renames && !describes && !explains) {
            forgetSaved()
            return
        }
        val written = id.value != null
        submit(onSuccess = forgetSaved) { itemId ->
            if (written && renames) items.rename(spaceId, itemId, title.orEmpty())
            if (written && describes) items.setDescription(spaceId, itemId, description.ifEmpty { null })
            if (written && explains) items.setDetails(spaceId, itemId, details.ifEmpty { null })
        }
    }

    fun openDelete() {
        if (uiState.value.canDelete) form.update { it.copy(confirmingDelete = true, failed = false) }
    }

    fun dismissDelete() = form.update { it.copy(confirmingDelete = false) }

    fun deleteItem() {
        if (!uiState.value.canDelete) return
        submit(onSuccess = { form.update { it.copy(deleted = true) } }) { itemId -> items.delete(spaceId, itemId) }
    }

    fun addChecklist() = submit { itemId -> items.setModule(spaceId, itemId, Checklist(emptyList())) }

    fun removeChecklist() = submit { itemId -> items.removeModule(spaceId, itemId, ModuleType.CHECKLIST) }

    /** A reminder counts back from a date, so without one the date comes first. */
    fun openReminder() {
        if (uiState.value.schedule == null) {
            editSchedule()
        } else {
            form.update { it.copy(editingReminder = true, failed = false) }
        }
    }

    fun dismissReminder() = form.update { it.copy(editingReminder = false, customReminder = null) }

    /** Adds the moment, or takes it off. */
    fun toggleReminderOffset(offset: Duration) {
        val current = uiState.value.reminderOffsets
        setReminderOffsets(if (offset in current) current - offset else current + offset)
    }

    /** Leaves the item without a reminder. */
    fun clearReminder() = setReminderOffsets(emptyList())

    /** Shows the fields for a moment of the user's own, or hides them. */
    fun toggleCustomReminder() = form.update {
        it.copy(customReminder = if (it.customReminder == null) CustomDuration() else null)
    }

    fun onCustomReminderAmountChange(text: String) = updateCustomReminder { it.withAmount(text) }

    fun selectCustomReminderUnit(unit: DurationUnit) = updateCustomReminder { it.copy(unit = unit) }

    /** Adds the moment typed, which then shows with the others. */
    fun addCustomReminder() {
        val offset = form.value.customReminder?.length ?: return
        form.update { it.copy(customReminder = null) }
        setReminderOffsets(uiState.value.reminderOffsets + offset)
    }

    /** Without any moments the reminder goes. The first one asks for notifications. */
    private fun setReminderOffsets(offsets: List<Duration>) {
        val first = uiState.value.reminderOffsets.isEmpty() && offsets.isNotEmpty()
        quickly(onSuccess = { if (first) form.update { it.copy(askNotifications = true) } }) { itemId ->
            if (offsets.isEmpty()) {
                items.removeModule(spaceId, itemId, ModuleType.REMINDER)
            } else {
                items.setModule(spaceId, itemId, Reminder(offsets.distinct().sorted()))
            }
        }
    }

    /** The user allowed notifications, or not: the reminder stays either way. */
    fun onNotificationsAnswered() = form.update { it.copy(askNotifications = false) }

    fun openMemberPicker() {
        if (uiState.value.canEdit) form.update { it.copy(pickingMember = true, failed = false) }
    }

    fun dismissMemberPicker() = form.update { it.copy(pickingMember = false, failed = false) }

    /** Gives the item to a member too, or takes it off them. Only space members can be picked. */
    fun toggleAssignee(memberId: String) {
        val current = uiState.value
        if (memberId !in current.memberNames) return
        val picked = current.assigneeIds
        val next = if (memberId in picked) picked - memberId else picked + memberId
        setAssignment(if (next.isEmpty()) null else Assignment.ToMembers(next))
    }

    /** Makes the item anyone's, or nobody's if it already was. */
    fun toggleAnyone() = setAssignment(if (uiState.value.isAnyones) null else Assignment.Anyone)

    private fun setAssignment(assignment: Assignment?) = quickly { itemId ->
        if (assignment == null) {
            items.removeModule(spaceId, itemId, ModuleType.ASSIGNMENT)
        } else {
            items.setModule(spaceId, itemId, assignment)
        }
    }

    /** Opens the schedule's editor on what it holds now, or on today for an item without one. */
    fun editSchedule() {
        if (!uiState.value.canEdit) return
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val schedule = uiState.value.schedule ?: Schedule(date = today)
        form.update { it.copy(scheduleEditor = ScheduleEditor.of(schedule, today), failed = false) }
    }

    /** Takes the date off the item, and with it the reminder that needs one. */
    fun removeSchedule() {
        val hasReminder = uiState.value.reminder != null
        form.update { it.copy(scheduleEditor = null) }
        submit { itemId ->
            if (hasReminder) items.removeModule(spaceId, itemId, ModuleType.REMINDER)
            items.removeModule(spaceId, itemId, ModuleType.SCHEDULE)
        }
    }

    fun dismissScheduleEditor() = form.update { it.copy(scheduleEditor = null) }

    fun setScheduleWindow(window: Boolean) = updateEditor { it.copy(window = window) }

    fun pickScheduleDate(which: ScheduleDate) = updateEditor { it.copy(picking = which) }

    fun dismissScheduleDatePicker() = updateEditor { it.copy(picking = null) }

    fun pickScheduleTime() = updateEditor { it.copy(pickingTime = true) }

    fun dismissScheduleTimePicker() = updateEditor { it.copy(pickingTime = false) }

    fun onScheduleTimePicked(hour: Int, minute: Int) = updateEditor {
        it.copy(time = "${hour.twoDigits()}:${minute.twoDigits()}", pickingTime = false)
    }

    /** Leaves the item without a time, so it is due at any point of its day. */
    fun clearScheduleTime() = updateEditor { it.copy(time = "", pickingTime = false) }

    fun onDurationHoursChange(hours: Int) = updateEditor { it.copy(durationHours = hours) }

    fun onDurationMinutesChange(minutes: Int) = updateEditor { it.copy(durationMinutes = minutes) }

    /** [utcMillis] is the UTC midnight M3's date picker returns. */
    fun onScheduleDatePicked(utcMillis: Long) = updateEditor { editor ->
        editor.withPicked(Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date)
    }

    /** Writes the edited schedule and closes the editor; a failure keeps it open with what was typed. */
    fun saveSchedule() {
        if (!uiState.value.canSaveSchedule) return
        val schedule = form.value.scheduleEditor?.schedule ?: return
        submit(onSuccess = { form.update { it.copy(scheduleEditor = null) } }) { itemId ->
            items.setModule(spaceId, itemId, schedule)
        }
    }

    fun editChecklistEntry(entryId: String) {
        if (!uiState.value.canEdit) return
        val entry = checklist()?.entries?.firstOrNull { it.id == entryId } ?: return
        form.update { it.copy(checklistEditor = ChecklistEditor.of(entry), failed = false) }
    }

    fun dismissChecklistEditor() {
        if (!form.value.submitting) form.update { it.copy(checklistEditor = null) }
    }

    fun onChecklistTextChange(text: String) = updateChecklistEditor { it.copy(text = text) }

    fun onChecklistQuantityChange(quantity: String) = updateChecklistEditor { it.copy(quantity = quantity) }

    fun saveChecklistEntry() {
        if (!uiState.value.canSaveChecklist) return
        val editor = form.value.checklistEditor ?: return
        if (checklist()?.entries?.none { it.id == editor.entryId } != false) return
        submit(onSuccess = { form.update { it.copy(checklistEditor = null) } }) { itemId ->
            items.updateEntry(spaceId, itemId, editor.entryId, editor.text.trim(), editor.quantityValue)
        }
    }

    fun onNewEntryChange(text: String) = form.update { it.copy(newEntry = text, failed = false) }

    /** Adds what was typed and clears the field at once, ready for the next one. A failure puts the text back. */
    fun addNewEntry() {
        if (!uiState.value.canAddEntry) return
        val checklist = checklist() ?: return
        val text = form.value.newEntry.trim()
        form.update { it.copy(newEntry = "") }
        quickly(onFailure = { form.update { it.copy(newEntry = it.newEntry.ifEmpty { text }) } }) { itemId ->
            items.addEntry(spaceId, itemId, text, null, checklist.nextOrder())
            if (uiState.value.isDone) items.reopen(spaceId, itemId)
        }
    }

    fun removeChecklistEntry() {
        val entryId = form.value.checklistEditor?.entryId ?: return
        if (checklist()?.entries?.any { it.id == entryId } != true) return
        submit(onSuccess = { form.update { it.copy(checklistEditor = null) } }) { itemId ->
            items.removeEntry(spaceId, itemId, entryId)
            settleStatus(itemId, checklist()?.entries.orEmpty().filterNot { it.id == entryId })
        }
    }

    /** The entry ticks at once and moves in the list a moment later, so it reads as one motion. */
    fun toggleChecklistEntry(entryId: String) {
        val entry = checklist()?.entries?.firstOrNull { it.id == entryId } ?: return
        form.update { it.copy(settling = it.settling + (entryId to entry.checked)) }
        viewModelScope.launch {
            delay(ENTRY_SETTLE)
            form.update { it.copy(settling = it.settling - entryId) }
        }
        quickly { itemId ->
            items.setEntryChecked(spaceId, itemId, entryId, !entry.checked)
            val entries = checklist()?.entries.orEmpty()
            settleStatus(itemId, entries.map { if (it.id == entryId) it.copy(checked = !entry.checked) else it })
        }
    }

    /** A checklist finishes the item: all of it ticked is done, and anything left opens it again (ADR 0008). */
    private suspend fun settleStatus(itemId: String, entries: List<ChecklistEntry>) {
        val finished = entries.isNotEmpty() && entries.all { it.checked }
        val done = uiState.value.isDone
        if (finished && !done) items.complete(spaceId, itemId)
        if (!finished && done) items.reopen(spaceId, itemId)
    }

    private fun checklist(): Checklist? = uiState.value.modules.filterIsInstance<Checklist>().firstOrNull()

    private fun Int.twoDigits(): String = toString().padStart(2, '0')

    private fun Checklist.nextOrder(): Int = entries.mapIndexed { index, entry -> entry.order ?: index }.maxOrNull()?.plus(1) ?: 0

    private fun updateCustomReminder(change: (CustomDuration) -> CustomDuration) = form.update {
        it.copy(customReminder = it.customReminder?.let(change), failed = false)
    }

    private fun updateChecklistEditor(change: (ChecklistEditor) -> ChecklistEditor) = form.update {
        it.copy(checklistEditor = it.checklistEditor?.let(change), failed = false)
    }

    private fun updateEditor(change: (ScheduleEditor) -> ScheduleEditor) = form.update {
        it.copy(scheduleEditor = it.scheduleEditor?.let(change), failed = false)
    }

    /** The item's id. A new item is written here the first time, with what was typed. */
    private suspend fun writtenId(): String = creating.withLock {
        id.value ?: run {
            val draft = form.value
            val description = draft.descriptionDraft?.trim()?.ifEmpty { null }
            val details = draft.detailsDraft?.trim()?.ifEmpty { null }
            val title = draft.titleDraft.orEmpty().trim().take(ITEM_TITLE_MAX_LENGTH)
            items.create(spaceId, NewItem(title, description, details = details)).also { id.value = it }
        }
    }

    private fun submit(onSuccess: () -> Unit = {}, write: suspend (String) -> Unit) {
        if (!uiState.value.canSubmit || form.value.submitting) return
        form.update { it.copy(submitting = true, failed = false) }
        viewModelScope.launch {
            try {
                val result = attempt("Changing item ${id.value} in space $spaceId") { write(writtenId()) }
                result.onSuccess { onSuccess() }
                form.update { it.copy(failed = result.isFailure) }
            } finally {
                form.update { it.copy(submitting = false) }
            }
        }
    }

    /**
     * For what is tapped several times in a row: ticking entries, picking people. A write can take the server a moment,
     * and these set a value rather than add one, so unlike [submit] this holds nothing back in the meantime.
     */
    private fun quickly(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}, write: suspend (String) -> Unit) {
        if (!uiState.value.canEdit) return
        form.update { it.copy(failed = false) }
        viewModelScope.launch {
            attempt("Changing item ${id.value} in space $spaceId") { write(writtenId()) }
                .onSuccess { onSuccess() }
                .onFailure {
                    onFailure()
                    form.update { it.copy(failed = true) }
                }
        }
    }

    private data class ItemContext(
        val spaceName: String? = null,
        val accent: Int? = null,
        val memberNames: Map<String, String> = emptyMap(),
        val members: List<Member> = emptyList(),
        val currentUserId: String? = null,
    )
}
