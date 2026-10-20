package com.orbit.app.feature.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.app.core.Load
import com.orbit.app.core.asLoad
import com.orbit.app.core.attempt
import com.orbit.app.core.orOnFailure
import com.orbit.app.core.valueOrNull
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.defaultModule
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

class ItemViewModel(
    private val spaceId: String,
    private val itemId: String,
    private val items: ItemRepository,
    spaces: SpaceRepository,
) : ViewModel() {
    private val action = MutableStateFlow(ActionState())
    private val scheduleEditor = MutableStateFlow<ScheduleEditor?>(null)
    private val askNotifications = MutableStateFlow(false)
    private val retries = Channel<Unit>(Channel.CONFLATED)

    private val item = items.observeItems(spaceId)
        .map { all -> all.firstOrNull { it.id == itemId } }
        .asLoad("Loading item $itemId in space $spaceId", retries)

    /** Only decorates the item, so a failed read leaves no space name and unknown members. */
    private val context = combine(
        spaces.observeSpaces().orOnFailure("Loading the spaces", emptyList()),
        spaces.observeMembers(spaceId).orOnFailure("Loading the members of $spaceId", emptyList()),
    ) { all, members ->
        ItemContext(
            spaceName = all.firstOrNull { it.id == spaceId }?.name,
            memberNames = members.associate { it.userId to it.displayName },
        )
    }.onStart { emit(ItemContext()) }

    val uiState: StateFlow<ItemUiState> = combine(
        item,
        action,
        context,
        scheduleEditor,
        askNotifications,
    ) { load, action, context, editor, ask ->
        val loaded = load.valueOrNull
        ItemUiState(
            title = loaded?.title.orEmpty(),
            description = loaded?.description,
            preset = loaded?.preset,
            status = loaded?.status ?: ItemStatus.Open,
            modules = loaded?.modules?.all.orEmpty(),
            loading = load == Load.Loading,
            loadFailed = load == Load.Failed,
            notFound = load is Load.Loaded && loaded == null,
            failed = action.failed,
            submitting = action.submitting,
            spaceName = context.spaceName,
            memberNames = context.memberNames,
            scheduleEditor = editor,
            askNotifications = ask,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemUiState())

    fun retry() {
        if (uiState.value.loadFailed) retries.trySend(Unit)
    }

    fun complete() = submit { items.complete(spaceId, itemId) }

    fun reopen() = submit { items.reopen(spaceId, itemId) }

    fun toggleDone(done: Boolean) = if (done) complete() else reopen()

    fun addModule(type: ModuleType) {
        if (type !in uiState.value.availableModules) return
        val module = defaultModule(type, Clock.System.todayIn(TimeZone.currentSystemDefault()))
        val reminds = type == ModuleType.REMINDER
        submit(onSuccess = { askNotifications.value = reminds }) { items.setModule(spaceId, itemId, module) }
    }

    /** The user allowed notifications, or not: the reminder stays either way. */
    fun onNotificationsAnswered() {
        askNotifications.value = false
    }

    /** Removing the schedule removes its reminder too: a reminder needs a schedule. */
    fun removeModule(type: ModuleType) {
        val current = uiState.value
        if (current.modules.none { it.type == type }) return
        val hasReminder = current.modules.any { it.type == ModuleType.REMINDER }
        submit {
            if (type == ModuleType.SCHEDULE && hasReminder) {
                items.removeModule(spaceId, itemId, ModuleType.REMINDER)
            }
            items.removeModule(spaceId, itemId, type)
        }
    }

    /** Opens the schedule's editor on what it holds now. */
    fun editSchedule() {
        val schedule = uiState.value.modules.filterIsInstance<Schedule>().firstOrNull() ?: return
        scheduleEditor.value = ScheduleEditor.of(schedule)
    }

    fun dismissScheduleEditor() {
        scheduleEditor.value = null
    }

    fun setScheduleWindow(window: Boolean) = updateEditor { it.copy(window = window) }

    fun onScheduleTimeChange(time: String) = updateEditor { it.copy(time = time) }

    fun onScheduleDurationChange(duration: String) = updateEditor { it.copy(duration = duration) }

    fun pickScheduleDate(which: ScheduleDate) = updateEditor { it.copy(picking = which) }

    fun dismissScheduleDatePicker() = updateEditor { it.copy(picking = null) }

    /** [utcMillis] is the UTC midnight M3's date picker returns. */
    fun onScheduleDatePicked(utcMillis: Long) = updateEditor { editor ->
        val date = Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date
        when (editor.picking) {
            ScheduleDate.START -> editor.copy(date = date, picking = null)
            ScheduleDate.END -> editor.copy(endDate = date, picking = null)
            null -> editor
        }
    }

    /** Writes the edited schedule and closes the editor; a failure keeps it open with what was typed. */
    fun saveSchedule() {
        if (!uiState.value.canSaveSchedule) return
        val schedule = scheduleEditor.value?.schedule ?: return
        submit(onSuccess = { scheduleEditor.value = null }) { items.setModule(spaceId, itemId, schedule) }
    }

    private fun updateEditor(change: (ScheduleEditor) -> ScheduleEditor) {
        scheduleEditor.update { it?.let(change) }
        action.update { it.copy(failed = false) }
    }

    private fun submit(onSuccess: () -> Unit = {}, write: suspend () -> Unit) {
        if (!uiState.value.canSubmit || action.value.submitting) return
        action.value = ActionState(submitting = true)
        viewModelScope.launch {
            try {
                val result = attempt("Changing item $itemId in space $spaceId", write)
                result.onSuccess { onSuccess() }
                action.value = ActionState(failed = result.isFailure)
            } finally {
                action.update { it.copy(submitting = false) }
            }
        }
    }

    private data class ActionState(val submitting: Boolean = false, val failed: Boolean = false)

    private data class ItemContext(
        val spaceName: String? = null,
        val memberNames: Map<String, String> = emptyMap(),
    )
}
