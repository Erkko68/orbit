package com.orbit.app.feature.item

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlinx.datetime.LocalDate

class ItemUiStateTest {
    @Test
    fun listProgressHandlesEmptyPartialAndCompletedLists() {
        val empty = ItemUiState(preset = Preset.SHOPPING_LIST, modules = listOf(Checklist(emptyList())))
        assertTrue(empty.showChecklistProgress)
        assertEquals(0f, empty.checklistProgress)
        val entries = listOf(
            com.orbit.app.domain.model.ChecklistEntry("milk", "Milk", true),
            com.orbit.app.domain.model.ChecklistEntry("eggs", "Eggs", false),
        )
        val partial = empty.copy(modules = listOf(Checklist(entries)))
        assertEquals(1, partial.checkedCount)
        assertEquals(2, partial.entryCount)
        assertEquals(0.5f, partial.checklistProgress)
        assertEquals(
            1f,
            partial.copy(modules = listOf(Checklist(entries.map { it.copy(checked = true) }))).checklistProgress
        )
        assertFalse(empty.copy(modules = emptyList()).showChecklistProgress)
        assertFalse(partial.copy(preset = Preset.TASK).showChecklistProgress)
    }

    @Test
    fun onlyEventsGetTheScheduleSummaryAndTodayLabel() {
        val date = LocalDate(2026, 10, 26)
        val event = ItemUiState(preset = Preset.EVENT, today = date, modules = listOf(Schedule(date)))
        assertTrue(event.eventIsToday)
        assertEquals(date, event.eventSchedule?.date)
        assertFalse(event.copy(today = LocalDate(2026, 10, 25)).eventIsToday)
        assertFalse(event.copy(modules = listOf(Schedule(date, endDate = LocalDate(2026, 10, 28)))).eventIsToday)
        assertNull(event.copy(preset = Preset.TASK).eventSchedule)
        assertFalse(ItemUiState().eventIsToday)
    }

    @Test
    fun unavailableItemsHaveNoActionsOrAddChips() {
        for (state in listOf(
            ItemUiState(),
            ItemUiState(loading = false, loadFailed = true),
            ItemUiState(loading = false, notFound = true),
        )) {
            assertFalse(state.canMutate)
            assertTrue(state.availableModules.isEmpty())
        }
        val ready = ItemUiState(loading = false)
        assertTrue(ready.canMutate)
        assertFalse(ready.copy(mutating = true).canMutate)
    }

    @Test
    fun onlyMissingModulesAreOfferedAndReminderNeedsSchedule() {
        val empty = ItemUiState(loading = false)
        assertEquals(listOf(ModuleType.SCHEDULE, ModuleType.ASSIGNMENT, ModuleType.CHECKLIST), empty.availableModules)
        val all = empty.copy(
            modules = listOf(
                Schedule(LocalDate(2026, 10, 26)),
                Assignment.Anyone,
                Checklist(emptyList()),
                Reminder(listOf(Duration.ZERO)),
            )
        )
        assertTrue(all.availableModules.isEmpty())
        assertEquals(listOf(ModuleType.REMINDER), all.copy(modules = all.modules.dropLast(1)).availableModules)
    }

    @Test
    fun descriptionsAndTaskCompletionFollowTheItem() {
        assertNull(ItemUiState().visibleDescription)
        assertNull(ItemUiState(description = "  ").visibleDescription)
        assertEquals("Kitchen leak", ItemUiState(description = "Kitchen leak").visibleDescription)
        assertTrue(ItemUiState().showCompletion)
        assertTrue(ItemUiState(preset = Preset.TASK).showCompletion)
        assertFalse(ItemUiState(preset = Preset.SHOPPING_LIST).showCompletion)
        assertFalse(ItemUiState(preset = Preset.EVENT).showCompletion)
    }
}
