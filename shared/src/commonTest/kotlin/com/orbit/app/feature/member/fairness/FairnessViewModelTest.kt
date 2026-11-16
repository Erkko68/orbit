package com.orbit.app.feature.member.fairness

import androidx.lifecycle.ViewModelStore
import com.orbit.app.domain.model.Completion
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemoryItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FairnessViewModelTest {
    private val items = InMemoryItemRepository(InMemoryAuthRepository())
    private val spaces = FakeSpaceRepository.withMembers("a" to Role.OWNER, "b" to Role.MEMBER)
    private val store = ViewModelStore()
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest
    fun tearDown() {
        store.clear(); Dispatchers.resetMain()
    }

    @Test
    fun readsAllRecentCompletionsAndShowsCurrentMembersAtZero() = runTest {
        val repo = object : ItemRepository by items {
            override fun observeRecentCompletions(spaceId: String, since: Instant): Flow<List<Completion>> = flow {
                emit((1..12).map { Completion("$it", "item", "a", Clock.System.now(), effortPoints = 1) })
            }
        }
        val model = FairnessViewModel("space-1", repo, spaces)
        store.put("fairness", model)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        assertFalse(model.uiState.value.loading)
        assertTrue(model.uiState.value.hasPoints)
        assertEquals(12L, model.uiState.value.share("a")?.points)
        assertEquals(0L, model.uiState.value.share("b")?.points)
    }

    @Test
    fun failedCompletionReadIsExplicitAndCanBeRetried() = runTest {
        var attempts = 0
        val repo = object : ItemRepository by items {
            override fun observeRecentCompletions(spaceId: String, since: Instant): Flow<List<Completion>> = flow {
                if (attempts++ == 0) error("offline")
                emit(emptyList())
            }
        }
        val model = FairnessViewModel("space-1", repo, spaces)
        store.put("fairness", model)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        assertTrue(model.uiState.value.loadFailed)
        model.retry()
        assertFalse(model.uiState.value.loadFailed)
        assertFalse(model.uiState.value.hasPoints)
    }

    @Test
    fun failedMembershipReadIsExplicitAndCanBeRetried() = runTest {
        var attempts = 0
        val repo = object : SpaceRepository by spaces {
            override fun observeMembers(spaceId: String) = flow {
                if (attempts++ == 0) error("offline")
                emit(spaces.observeMembers(spaceId).first())
            }
        }
        val model = FairnessViewModel("space-1", items, repo)
        store.put("fairness", model)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect {} }
        assertTrue(model.uiState.value.loadFailed)
        model.retry()
        assertFalse(model.uiState.value.loadFailed)
    }
}
