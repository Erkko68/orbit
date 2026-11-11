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
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.usecase.rolledForward
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** How long a ticked entry stays where it is, so the tick is seen before the row moves. */
private val ENTRY_SETTLE = 450.milliseconds

/**
 * One item, or with an empty [itemId] a new one. A new item keeps everything here, in any order, until [create] writes
 * it; leaving before that leaves nothing behind. A written item saves each change at once.
 */
class ItemViewModel(
    private val spaceId: String,
    private val itemId: String,
    private val items: ItemRepository,
    spaces: SpaceRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val isNew = itemId.isEmpty()

    /** What the user is doing on the screen. The rest stays empty: [uiState] fills it from the repositories. */
    private val form = MutableStateFlow(ItemUiState(isNew = isNew))

    /** The modules of the new item, until it is created. */
    private val draft = MutableStateFlow(ItemModules())
    private val retries = Channel<Unit>(Channel.CONFLATED)

    private val item: Flow<Load<Item?>> = if (isNew) {
        flowOf(Load.Loaded(null))
    } else {
        items.observeItems(spaceId).map { all -> all.firstOrNull { it.id == itemId } }
            .asLoad("Loading item $itemId in space $spaceId", retries)
    }

    /** Only decorates the item, so a failed read leaves no space name and unknown members. */
    private val context = combine(
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
        auth.session,
    ) { all, members, user ->
        val space = all.firstOrNull { it.id == spaceId }
        ItemContext(space?.name, members, user?.id)
    }.onStart { emit(ItemContext()) }

    val uiState: StateFlow<ItemUiState> = combine(item, context, form, draft) { load, context, form, draft ->
        val loaded = load.valueOrNull
        form.copy(
            title = loaded?.title.orEmpty(),
            createdBy = loaded?.createdBy,
            description = loaded?.description,
            status = loaded?.status ?: form.status,
            modules = (loaded?.modules ?: draft).all,
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            notFound = !isNew && load is Load.Loaded && loaded == null,
            spaceName = context.spaceName,
            memberNames = context.members.associate { it.userId to it.displayName },
            members = context.members,
            currentUserId = context.currentUserId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemUiState(isNew = isNew, loading = !isNew))

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    /** Writes the new item with everything given to it, then the screen closes. A failure keeps it all. */
    fun create() {
        val state = uiState.value
        if (!state.canCreate) return
        val item = NewItem(
            title = state.shownTitle.trim().take(ITEM_TITLE_MAX_LENGTH),
            description = state.descriptionDraft?.trim()?.ifEmpty { null },
            modules = draft.value,
        )
        submit(onSuccess = { form.update { it.copy(created = true) } }) { items.create(spaceId, item) }
    }

    /** A recurring item moves to its next date and stays open (ADR 0011). */
    fun complete() = submit { items.complete(spaceId, itemId, modules().rolledForward(today())) }

    fun reopen() = submit { items.reopen(spaceId, itemId) }

    fun onTitleChange(text: String) = form.update { it.copy(titleDraft = text, failed = false) }

    fun onDescriptionChange(text: String) = form.update { it.copy(descriptionDraft = text, failed = false) }

    /**
     * Saves what was typed on a written item, in one go whichever field the user left. A blank title goes back to the
     * one it had, and a failure keeps the text. A new item keeps it all until it is created.
     */
    fun saveDrafts() {
        if (isNew) return
        val current = uiState.value
        val title = form.value.titleDraft?.trim()?.take(ITEM_TITLE_MAX_LENGTH)
        val description = form.value.descriptionDraft?.trim()
        val renames = !title.isNullOrEmpty() && title != current.title
        val describes = description != null && description != current.description.orEmpty()
        // Only what was saved: the user may already be typing in the other field.
        val forgetSaved = {
            form.update {
                it.copy(
                    titleDraft = it.titleDraft.takeUnless { draft -> draft?.trim()?.take(ITEM_TITLE_MAX_LENGTH) == title },
                    descriptionDraft = it.descriptionDraft.takeUnless { draft -> draft?.trim() == description },
                )
            }
        }
        if (!renames && !describes) {
            forgetSaved()
            return
        }
        quickly(onSuccess = forgetSaved) {
            if (renames) items.rename(spaceId, itemId, title.orEmpty())
            if (describes) items.setDescription(spaceId, itemId, description.ifEmpty { null })
        }
    }

    fun openDelete() {
        if (uiState.value.canDelete) form.update { it.copy(confirmingDelete = true, failed = false) }
    }

    fun dismissDelete() = form.update { it.copy(confirmingDelete = false) }

    fun deleteItem() {
        if (!uiState.value.canDelete) return
        submit(onSuccess = { form.update { it.copy(deleted = true) } }) { items.delete(spaceId, itemId) }
    }

    /** A reminder counts back from a date, so there is nothing to open without one. */
    fun openReminder() {
        if (uiState.value.showsReminder) form.update { it.copy(editingReminder = true, addingReminder = false, failed = false) }
    }

    fun dismissReminder() = form.update { it.copy(editingReminder = false, addingReminder = false, customReminder = CustomDuration()) }

    fun openAddReminder() = form.update { it.copy(addingReminder = true, customReminder = CustomDuration(), failed = false) }

    fun closeAddReminder() = form.update { it.copy(addingReminder = false) }

    /** Adds the moment to the list, which the sheet then shows again. */
    fun addReminderOffset(offset: Duration) {
        form.update { it.copy(addingReminder = false) }
        setReminderOffsets(uiState.value.reminderOffsets + offset)
    }

    /** Takes the moment off the list. Without any left the reminder goes. */
    fun removeReminderOffset(offset: Duration) = setReminderOffsets(uiState.value.reminderOffsets - offset)

    fun onCustomReminderAmountChange(amount: Int) = updateCustomReminder { it.copy(amount = amount) }

    fun selectCustomReminderUnit(unit: DurationUnit) = updateCustomReminder { it.copy(unit = unit) }

    /** Adds the moment on the wheels. */
    fun addCustomReminder() = form.value.customReminder.length?.let(::addReminderOffset)

    /** Without any moments the reminder goes. The first one asks for notifications. */
    private fun setReminderOffsets(offsets: List<Duration>) {
        val first = uiState.value.reminderOffsets.isEmpty() && offsets.isNotEmpty()
        val reminder = Reminder(offsets.distinct().sorted())
        edit(
            local = { if (offsets.isEmpty()) it.without(ModuleType.REMINDER) else it.with(reminder) },
            onSuccess = { if (first) form.update { it.copy(askNotifications = true) } },
        ) {
            if (offsets.isEmpty()) {
                items.removeModule(spaceId, itemId, ModuleType.REMINDER)
            } else {
                items.setModule(spaceId, itemId, reminder)
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

    private fun setAssignment(assignment: Assignment?) = edit(
        local = { if (assignment == null) it.without(ModuleType.ASSIGNMENT) else it.with(assignment) },
    ) {
        if (assignment == null) {
            items.removeModule(spaceId, itemId, ModuleType.ASSIGNMENT)
        } else {
            items.setModule(spaceId, itemId, assignment)
        }
    }

    /** Opens the schedule's editor on what it holds now, or on today for an item without one. */
    fun editSchedule() {
        if (!uiState.value.canEdit) return
        val today = today()
        val state = uiState.value
        val schedule = state.schedule ?: Schedule(date = today)
        form.update { it.copy(scheduleEditor = ScheduleEditor.of(schedule, state.recurrence, today), failed = false) }
    }

    /** Takes the date off the item, and with it the reminder and the repeat that need one. */
    fun removeSchedule() {
        val hasReminder = uiState.value.reminder != null
        val hasRecurrence = uiState.value.recurrence != null
        form.update { it.copy(scheduleEditor = null) }
        edit(local = { it.without(ModuleType.REMINDER).without(ModuleType.RECURRENCE).without(ModuleType.SCHEDULE) }) {
            if (hasReminder) items.removeModule(spaceId, itemId, ModuleType.REMINDER)
            if (hasRecurrence) items.removeModule(spaceId, itemId, ModuleType.RECURRENCE)
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

    fun openRepeat() = updateEditor { it.copy(editingRepeat = true) }

    fun closeRepeat() = updateEditor { it.copy(editingRepeat = false) }

    /** Repeats in one of the usual ways, or not at all, and goes back to the schedule. */
    fun pickRepeat(recurrence: Recurrence?) = updateEditor { it.withRecurrence(recurrence).copy(editingRepeat = false) }

    /** Opens the wheels, on weeks when it does not repeat yet. */
    fun openCustomRepeat() = updateEditor { it.copy(customRepeat = true, repeat = it.repeat ?: RepeatUnit.WEEKS) }

    fun closeCustomRepeat() = updateEditor { it.copy(customRepeat = false) }

    /** Keeps the repeat on the wheels and goes back to the schedule. */
    fun finishCustomRepeat() = updateEditor { it.copy(customRepeat = false, editingRepeat = false) }

    fun selectRepeatUnit(unit: RepeatUnit) = updateEditor { it.copy(repeat = unit) }

    fun onRepeatEveryChange(every: Int) = updateEditor { it.copy(repeatEvery = every.coerceAtLeast(1)) }

    fun toggleRepeatWeekday(day: DayOfWeek) = updateEditor { it.withWeekdayToggled(day) }

    /** Repeats for good, with no last day, and leaves the calendar. */
    fun clearRepeatUntil() = updateEditor { it.copy(repeatUntil = null, picking = null) }

    fun onDurationHoursChange(hours: Int) = updateEditor { it.copy(durationHours = hours) }

    fun onDurationMinutesChange(minutes: Int) = updateEditor { it.copy(durationMinutes = minutes) }

    /** [utcMillis] is the UTC midnight M3's date picker returns. */
    fun onScheduleDatePicked(utcMillis: Long) = updateEditor { editor ->
        editor.withPicked(Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date)
    }

    /** Keeps the edited schedule and its repeat and closes the editor; a failure keeps it open with what was typed. */
    fun saveSchedule() {
        if (!uiState.value.canSaveSchedule) return
        val editor = form.value.scheduleEditor ?: return
        val schedule = editor.schedule ?: return
        val recurrence = editor.recurrence
        val hadRecurrence = uiState.value.recurrence != null
        edit(
            local = { modules ->
                modules.with(schedule).let { if (recurrence == null) it.without(ModuleType.RECURRENCE) else it.with(recurrence) }
            },
            onSuccess = { form.update { it.copy(scheduleEditor = null) } },
        ) {
            items.setModule(spaceId, itemId, schedule)
            if (recurrence != null) {
                items.setModule(spaceId, itemId, recurrence)
            } else if (hadRecurrence) {
                items.removeModule(spaceId, itemId, ModuleType.RECURRENCE)
            }
        }
    }

    fun editChecklistEntry(entryId: String) {
        if (!uiState.value.canEdit) return
        val entry = entries().firstOrNull { it.id == entryId } ?: return
        form.update { it.copy(checklistEditor = ChecklistEditor.of(entry), failed = false) }
    }

    fun dismissChecklistEditor() = form.update { it.copy(checklistEditor = null) }

    fun onChecklistTextChange(text: String) = updateChecklistEditor { it.copy(text = text) }

    fun onChecklistQuantityChange(quantity: String) = updateChecklistEditor { it.copy(quantity = quantity) }

    fun saveChecklistEntry() {
        if (!uiState.value.canSaveChecklist) return
        val editor = form.value.checklistEditor ?: return
        if (entries().none { it.id == editor.entryId }) return
        val text = editor.text.trim()
        edit(
            local = { modules ->
                modules.withEntries { all ->
                    all.map { if (it.id == editor.entryId) it.copy(text = text, quantity = editor.quantityValue) else it }
                }
            },
            onSuccess = { form.update { it.copy(checklistEditor = null) } },
        ) { items.updateEntry(spaceId, itemId, editor.entryId, text, editor.quantityValue) }
    }

    fun onNewEntryChange(text: String) = form.update { it.copy(newEntry = text, failed = false) }

    /** Adds what was typed and clears the field at once, ready for the next one. A failure puts the text back. */
    fun addNewEntry() {
        if (!uiState.value.canAddEntry) return
        val text = form.value.newEntry.trim()
        val order = entries().nextOrder()
        form.update { it.copy(newEntry = "") }
        edit(
            local = { modules -> modules.withEntries { it + ChecklistEntry("entry-$order", text, order = order) } },
            onFailure = { form.update { it.copy(newEntry = it.newEntry.ifEmpty { text }) } },
        ) {
            items.addEntry(spaceId, itemId, text, null, order)
            if (uiState.value.isDone) items.reopen(spaceId, itemId)
        }
    }

    fun removeChecklistEntry() {
        val entryId = form.value.checklistEditor?.entryId ?: return
        if (entries().none { it.id == entryId }) return
        edit(
            local = { modules -> modules.withEntries { all -> all.filterNot { it.id == entryId } } },
            onSuccess = { form.update { it.copy(checklistEditor = null) } },
        ) {
            items.removeEntry(spaceId, itemId, entryId)
            settleStatus(entries().filterNot { it.id == entryId })
        }
    }

    /** The entry ticks at once and moves in the list a moment later, so it reads as one motion. */
    fun toggleChecklistEntry(entryId: String) {
        val entry = entries().firstOrNull { it.id == entryId } ?: return
        form.update { it.copy(settling = it.settling + (entryId to entry.checked)) }
        viewModelScope.launch {
            delay(ENTRY_SETTLE)
            form.update { it.copy(settling = it.settling - entryId) }
        }
        val toggled: (List<ChecklistEntry>) -> List<ChecklistEntry> = { all ->
            all.map { if (it.id == entryId) it.copy(checked = !entry.checked) else it }
        }
        edit(local = { it.withEntries(toggled) }) {
            items.setEntryChecked(spaceId, itemId, entryId, !entry.checked)
            settleStatus(toggled(entries()))
        }
    }

    /** A checklist finishes the item: all of it ticked is done, and anything left opens it again (ADR 0008). */
    private suspend fun settleStatus(entries: List<ChecklistEntry>) {
        val finished = entries.isNotEmpty() && entries.all { it.checked }
        val done = uiState.value.isDone
        // ponytail: a finished list closes even when it recurs; rolling it forward needs its entries unticked too.
        if (finished && !done) items.complete(spaceId, itemId, null)
        if (!finished && done) items.reopen(spaceId, itemId)
    }

    private fun modules(): ItemModules = uiState.value.modules.fold(ItemModules(), ItemModules::with)

    private fun today() = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private fun entries(): List<ChecklistEntry> =
        uiState.value.modules.filterIsInstance<Checklist>().firstOrNull()?.entries.orEmpty()

    private fun ItemModules.withEntries(change: (List<ChecklistEntry>) -> List<ChecklistEntry>): ItemModules =
        with(Checklist(change(checklist?.entries.orEmpty())))

    private fun Int.twoDigits(): String = toString().padStart(2, '0')

    private fun List<ChecklistEntry>.nextOrder(): Int =
        mapIndexed { index, entry -> entry.order ?: index }.maxOrNull()?.plus(1) ?: 0

    private fun updateCustomReminder(change: (CustomDuration) -> CustomDuration) = form.update {
        it.copy(customReminder = change(it.customReminder), failed = false)
    }

    private fun updateChecklistEditor(change: (ChecklistEditor) -> ChecklistEditor) = form.update {
        it.copy(checklistEditor = it.checklistEditor?.let(change), failed = false)
    }

    private fun updateEditor(change: (ScheduleEditor) -> ScheduleEditor) = form.update {
        it.copy(scheduleEditor = it.scheduleEditor?.let(change), failed = false)
    }

    /** A change to the modules: the new item keeps it in [draft] through [local], a written one saves it with [write]. */
    private fun edit(
        local: (ItemModules) -> ItemModules,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {},
        write: suspend () -> Unit,
    ) {
        if (!uiState.value.canEdit) return
        if (isNew) {
            draft.update(local)
            onSuccess()
        } else {
            quickly(onSuccess, onFailure, write)
        }
    }

    /** For what holds the screen until it is written, such as Create or Mark done, so it cannot run twice. */
    private fun submit(onSuccess: () -> Unit = {}, write: suspend () -> Unit) {
        if (!uiState.value.loaded || form.value.submitting) return
        form.update { it.copy(submitting = true, failed = false) }
        viewModelScope.launch {
            val result = attempt("Changing item $itemId in space $spaceId") { write() }
            result.onSuccess { onSuccess() }
            form.update { it.copy(submitting = false, failed = result.isFailure) }
        }
    }

    /**
     * For what is tapped several times in a row: ticking entries, picking people. A write can take the server a moment,
     * and these set a value rather than add one, so unlike [submit] this holds nothing back in the meantime.
     */
    private fun quickly(onSuccess: () -> Unit = {}, onFailure: () -> Unit = {}, write: suspend () -> Unit) {
        form.update { it.copy(failed = false) }
        viewModelScope.launch {
            attempt("Changing item $itemId in space $spaceId") { write() }
                .onSuccess { onSuccess() }
                .onFailure {
                    onFailure()
                    form.update { it.copy(failed = true) }
                }
        }
    }

    private data class ItemContext(
        val spaceName: String? = null,
        val members: List<Member> = emptyList(),
        val currentUserId: String? = null,
    )
}
