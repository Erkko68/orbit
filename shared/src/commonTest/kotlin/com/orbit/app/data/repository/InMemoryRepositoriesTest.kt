package com.orbit.app.data.repository

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class InMemoryRepositoriesTest {

    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)
    private val items = InMemoryItemRepository(auth)

    private suspend fun onlyItem() = items.observeItems("space-1").first().single()

    @Test
    fun sessionFollowsSignInAndSignOut() = runTest {
        assertNull(auth.session.first())

        auth.signIn("ana@orbit.app", "secret")
        assertEquals("ana", auth.session.first()?.displayName)

        auth.signOut()
        assertNull(auth.session.first())
    }

    @Test
    fun aSpaceIsOnlyVisibleToItsMembers() = runTest {
        auth.register("ana@orbit.app", "secret", displayName = "Ana")
        val id = spaces.createSpace("Flat", accent = 2)

        val space = spaces.observeSpaces().first().single()
        assertEquals(id, space.id)
        assertEquals(listOf("ana@orbit.app"), space.memberIds)
        val owner = spaces.observeMembers(id).first().single()
        assertEquals(Role.OWNER, owner.role)
        assertEquals("Ana", owner.displayName)

        auth.signIn("ben@orbit.app", "secret")
        assertEquals(emptyList(), spaces.observeSpaces().first())
        auth.signOut()
        assertEquals(emptyList(), spaces.observeSpaces().first())
    }

    @Test
    fun writesNeedASignedInUser() = runTest {
        assertFailsWith<IllegalStateException> { spaces.createSpace("Flat", accent = 0) }
        assertFailsWith<IllegalStateException> { items.create("space-1", NewItem("Buy milk")) }
    }

    @Test
    fun itemsAreCreatedPerSpaceAndRenamed() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = items.create("space-1", NewItem("Buy milk"))
        items.create("space-2", NewItem("Pay rent"))

        assertEquals(id, onlyItem().id)
        assertEquals("ana@orbit.app", onlyItem().createdBy)
        assertEquals(ItemStatus.Open, onlyItem().status)

        items.rename("space-1", id, "Buy oat milk")
        assertEquals("Buy oat milk", onlyItem().title)
        assertFailsWith<NoSuchElementException> { items.rename("space-2", id, "Wrong space") }
    }

    @Test
    fun modulesAreSetAndRemoved() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = items.create("space-1", NewItem("Buy milk"))

        items.setModule("space-1", id, Assignment.Anyone)
        assertEquals(ItemModules(assignment = Assignment.Anyone), onlyItem().modules)

        items.removeModule("space-1", id, ModuleType.ASSIGNMENT)
        assertEquals(ItemModules(), onlyItem().modules)
    }

    @Test
    fun aChecklistEntryIsTicked() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val checklist = Checklist(listOf(ChecklistEntry("1", "Milk"), ChecklistEntry("2", "Eggs")))
        val id = items.create("space-1", NewItem("Shopping", modules = ItemModules(checklist = checklist)))

        items.setEntryChecked("space-1", id, entryId = "2", checked = true)
        assertEquals(listOf(false, true), onlyItem().modules.checklist?.entries?.map { it.checked })

        val plain = items.create("space-2", NewItem("No checklist"))
        assertFailsWith<IllegalStateException> { items.setEntryChecked("space-2", plain, "1", true) }
    }

    @Test
    fun completingRecordsWhoAndReopeningClearsIt() = runTest {
        auth.signIn("ana@orbit.app", "secret")
        val id = items.create("space-1", NewItem("Buy milk"))

        items.complete("space-1", id)
        assertEquals("ana@orbit.app", assertIs<ItemStatus.Done>(onlyItem().status).by)

        items.reopen("space-1", id)
        assertEquals(ItemStatus.Open, onlyItem().status)
    }
}
