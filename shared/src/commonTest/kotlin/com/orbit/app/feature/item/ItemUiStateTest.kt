package com.orbit.app.feature.item

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ModuleType
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
    fun blankDescriptionsAreHidden() {
        assertNull(ItemUiState().visibleDescription)
        assertNull(ItemUiState(description = "  ").visibleDescription)
        assertEquals("Kitchen leak", ItemUiState(description = "Kitchen leak").visibleDescription)
    }

    @Test
    fun avatarFollowsTheAssignedMemberInBothAssignmentModes() {
        val state = ItemUiState(
            modules = listOf(Assignment.ToMember("ana")),
            memberNames = mapOf("ana" to "Ana López", "mika" to "Mika Rossi"),
        )
        assertEquals("Ana López", state.assigneeName)
        assertEquals("AL", state.assigneeInitials)

        val claimed = state.copy(modules = listOf(Assignment.Claim("mika")))
        assertEquals("Mika Rossi", claimed.assigneeName)
        assertEquals("MR", claimed.assigneeInitials)
    }

    @Test
    fun absentOrUnresolvedAssigneesHaveNoInventedAvatar() {
        val members = mapOf("ana" to "Ana López", "blank" to "  ")
        for (modules in listOf(
            emptyList(),
            listOf(Assignment.Anyone),
            listOf(Assignment.Claim()),
            listOf(Assignment.ToMember("unknown")),
            listOf(Assignment.ToMember("blank")),
        )) {
            val state = ItemUiState(modules = modules, memberNames = members)
            assertNull(state.assigneeName)
            assertNull(state.assigneeInitials)
        }
    }
}
