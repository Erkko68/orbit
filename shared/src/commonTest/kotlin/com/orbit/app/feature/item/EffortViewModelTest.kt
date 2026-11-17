package com.orbit.app.feature.item

import androidx.lifecycle.ViewModelStore
import com.orbit.app.core.PlanDraft
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Effort
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Module
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EffortViewModelTest {
    private val auth = InMemoryAuthRepository()
    private val items = InMemoryItemRepository(auth)
    private val spaces = FakeSpaceRepository.withMembers("a" to Role.OWNER)
    private val store = ViewModelStore()
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest
    fun tearDown() {
        store.clear(); Dispatchers.resetMain()
    }

    private fun TestScope.model(id: String, repo: ItemRepository = items) =
        ItemViewModel("space-1", id, "", repo, spaces, auth, PlanDraft()).also {
            store.put("item", it)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { it.uiState.collect {} }
        }

    @Test
    fun effortIsOptionalAndTheNewItemKeepsItOnlyAfterConfirmation() = runTest {
        auth.signIn("a", "secret")
        val model = model("")
        assertNull(model.uiState.value.effort)
        model.openEffort()
        assertEquals(1, model.uiState.value.effortDraft)
        model.selectEffort(4)
        model.dismissEffort()
        assertNull(model.uiState.value.effort)
        model.openEffort()
        model.selectEffort(0)
        assertEquals(1, model.uiState.value.effortDraft)
        model.saveEffort()
        assertEquals(Effort(1), model.uiState.value.effort)
        assertTrue(items.observeItems("space-1").first().isEmpty())
        model.onTitleChange("Bins")
        model.create()
        assertEquals(Effort(1), items.observeItems("space-1").first().single().modules.effort)
    }

    @Test
    fun aFailedSaveKeepsTheDraftAndRetryCanSaveAndRemoveTheModule() = runTest {
        auth.signIn("a", "secret")
        val id = items.create("space-1", NewItem("Bins"))
        var fail = true
        val repo = object : ItemRepository by items {
            override suspend fun setModule(spaceId: String, itemId: String, module: Module) {
                if (fail) error("denied")
                items.setModule(spaceId, itemId, module)
            }

            override suspend fun removeModule(spaceId: String, itemId: String, type: ModuleType) {
                if (fail) error("denied")
                items.removeModule(spaceId, itemId, type)
            }
        }
        val model = model(id, repo)
        model.openEffort()
        model.selectEffort(5)
        model.saveEffort()
        assertTrue(model.uiState.value.failed)
        assertTrue(model.uiState.value.editingEffort)
        assertEquals(5, model.uiState.value.effortDraft)
        fail = false
        model.saveEffort()
        assertFalse(model.uiState.value.editingEffort)
        assertEquals(Effort(5), model.uiState.value.effort)
        model.openEffort()
        model.clearEffort()
        fail = true
        model.saveEffort()
        assertTrue(model.uiState.value.editingEffort)
        assertEquals(Effort(5), model.uiState.value.effort)
        fail = false
        model.saveEffort()
        assertNull(model.uiState.value.effort)
    }

    @Test
    fun aPendingSaveBlocksDuplicateSubmissionAndFinishesAfterDismissal() = runTest {
        auth.signIn("a", "secret")
        val id = items.create("space-1", NewItem("Bins"))
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val repo = object : ItemRepository by items {
            override suspend fun setModule(spaceId: String, itemId: String, module: Module) {
                calls++
                gate.await()
                items.setModule(spaceId, itemId, module)
            }
        }
        val model = model(id, repo)
        model.openEffort()
        model.saveEffort()
        model.saveEffort()
        model.dismissEffort()
        assertEquals(1, calls)
        assertFalse(model.uiState.value.editingEffort)
        assertFalse(model.uiState.value.canSaveEffort)
        gate.complete(Unit)
        assertFalse(model.uiState.value.editingEffort)
    }

    @Test
    fun completionKeepsItsOriginalSnapshotAndActualCompleterAfterReopening() = runTest {
        auth.signIn("a", "secret")
        val id = items.create(
            "space-1", NewItem(
                "Bins", modules = ItemModules(
                    effort = Effort(3), assignment = Assignment.ToMembers(setOf("someone-else")),
                )
            )
        )
        items.complete("space-1", id, null)
        val original = items.observeCompletions("space-1", id).first().single()
        items.reopen("space-1", id)
        items.setModule("space-1", id, Effort(5))
        items.complete("space-1", id, null)
        val history = items.observeCompletions("space-1", id).first()
        assertEquals(listOf(original), history.filter { it.effortPoints != null && it.effortCredited })
        assertEquals(5, history.first().effortPoints)
        assertFalse(history.first().effortCredited)
        assertEquals("a", original.completedBy)
        assertEquals(3, original.effortPoints)
        items.removeModule("space-1", id, ModuleType.EFFORT)
        assertEquals(original, items.observeCompletions("space-1", id).first().last())
    }

    @Test
    fun noModuleEarnsNoPointsEvenIfEffortIsAddedAfterReopening() = runTest {
        auth.signIn("a", "secret")
        val id = items.create("space-1", NewItem("Bins"))
        items.complete("space-1", id, null)
        items.reopen("space-1", id)
        items.setModule("space-1", id, Effort(4))
        items.complete("space-1", id, null)
        assertTrue(
            items.observeCompletions("space-1", id).first().none { it.effortPoints != null && it.effortCredited })
    }

    @Test
    fun rollingForwardStartsANewOccurrenceWithItsOwnEffortSnapshot() = runTest {
        auth.signIn("a", "secret")
        val id = items.create("space-1", NewItem("Bins", modules = ItemModules(effort = Effort(2))))
        items.complete("space-1", id, Schedule(LocalDate(2026, 11, 17)))
        items.setModule("space-1", id, Effort(4))
        items.complete("space-1", id, null)
        assertEquals(listOf(4, 2), items.observeCompletions("space-1", id).first().map { it.effortPoints })
    }
}
