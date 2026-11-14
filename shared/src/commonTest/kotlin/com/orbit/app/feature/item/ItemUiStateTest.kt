package com.orbit.app.feature.item

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemUiStateTest {
    @Test
    fun onlyAnExistingAvailableChecklistCanBeAddedAsExpense() {
        val ready = ItemUiState(loading = false, modules = listOf(Checklist(emptyList())))
        assertTrue(ready.canAddExpense)
        for (state in listOf(
            ready.copy(isNew = true), ready.copy(loading = true), ready.copy(loadFailed = true),
            ready.copy(notFound = true), ready.copy(submitting = true), ready.copy(modules = emptyList()),
        )) assertFalse(state.canAddExpense)
    }

    @Test
    fun unavailableItemsHaveNoActions() {
        for (state in listOf(
            ItemUiState(),
            ItemUiState(loading = false, loadFailed = true),
            ItemUiState(loading = false, notFound = true),
        )) {
            assertFalse(state.canSubmit)
        }
        val ready = ItemUiState(loading = false)
        assertTrue(ready.canSubmit)
        assertFalse(ready.copy(submitting = true).canSubmit)
    }

    @Test
    fun aNewItemFillsInAnyOrderAndCreatesOnceNamed() {
        val new = ItemUiState(loading = false, isNew = true)
        assertTrue(new.canEdit)
        assertFalse(new.canCreate)
        assertFalse(new.copy(titleDraft = "  ").canCreate)
        assertTrue(new.copy(titleDraft = "Groceries").canCreate)
        assertFalse(new.copy(titleDraft = "Groceries", submitting = true).canCreate)
        assertFalse(new.showsDone)
    }

    @Test
    fun markDoneShowsOnAWrittenItemWithoutEntries() {
        val written = ItemUiState(loading = false)
        assertTrue(written.showsDone)
        assertFalse(written.copy(modules = listOf(Checklist(listOf(ChecklistEntry("a", "Milk"))))).showsDone)
        assertTrue(written.copy(modules = listOf(Checklist(emptyList()))).showsDone)
    }

    @Test
    fun whoShowsFollowsTheAssignment() {
        val members = listOf(Member("ana", "Ana", null, Role.OWNER), Member("mika", "Mika", null, Role.MEMBER))
        val state = ItemUiState(members = members)

        assertTrue(state.assigneeFaces.isEmpty())
        assertEquals(listOf("ana", "mika"), state.copy(modules = listOf(Assignment.Anyone)).assigneeFaces.map { it.id })
        val both = state.copy(modules = listOf(Assignment.ToMembers(setOf("mika", "ana", "gone"))))
        assertEquals(listOf("ana", "mika"), both.assigneeFaces.map { it.id })
    }

    @Test
    fun anythingWithAChecklistShowsItsEntriesAndProgress() {
        val entries = listOf(ChecklistEntry("a", "Milk", checked = true), ChecklistEntry("b", "Bread"))
        val state = ItemUiState(modules = listOf(Checklist(entries)))

        assertTrue(state.hasEntries)
        assertEquals(listOf("b", "a"), state.shownEntries.map { it.id })
        assertEquals(1, state.checkedCount)
        assertEquals(2, state.entryCount)
    }

    @Test
    fun anEntryJustTickedKeepsItsPlaceButCountsAsTicked() {
        val entries = listOf(ChecklistEntry("a", "Milk", checked = true), ChecklistEntry("b", "Bread"))
        val state = ItemUiState(modules = listOf(Checklist(entries)), settling = mapOf("a" to false))

        assertEquals(listOf("a", "b"), state.shownEntries.map { it.id })
        assertEquals(1, state.checkedCount)
    }

    @Test
    fun aMomentOfTheUsersOwnJoinsTheChoices() {
        val state = ItemUiState()
        assertEquals(4, state.reminderChoices.size)
    }
}
