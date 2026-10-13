package com.orbit.app.feature.spacehome.members

import com.orbit.app.data.repository.InMemoryAuthRepository
import com.orbit.app.data.repository.InMemorySpaceRepository
import com.orbit.app.domain.model.Role
import com.orbit.app.testing.FakeSpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.role_admin
import orbit.shared.generated.resources.role_member
import orbit.shared.generated.resources.role_owner
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceMembersViewModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun showsTheOwnerOfANewSpace() = runTest {
        val auth = InMemoryAuthRepository()
        val spaces = InMemorySpaceRepository(auth)
        auth.register("ana@orbit.app", "secret", displayName = "Ana López")
        val id = spaces.createSpace("Flat 4B", accent = 0)

        val state = SpaceMembersViewModel(id, spaces).uiState.first { it.members.isNotEmpty() }

        assertEquals(listOf(SpaceMember("ana@orbit.app", "Ana López", "AL", Res.string.role_owner)), state.members)
    }

    @Test
    fun labelsEveryRole() = runTest {
        val repository = FakeSpaceRepository.withMembers("Ana" to Role.OWNER, "Ben" to Role.ADMIN, "Cy" to Role.MEMBER)

        val state = SpaceMembersViewModel("space-1", repository).uiState.first { it.members.isNotEmpty() }

        assertEquals(
            listOf(Res.string.role_owner, Res.string.role_admin, Res.string.role_member),
            state.members.map { it.role },
        )
    }
}
