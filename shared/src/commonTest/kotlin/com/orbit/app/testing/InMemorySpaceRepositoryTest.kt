package com.orbit.app.testing

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The in-memory repository refuses a role change as the security rules do, so a test cannot rely on a
 * change the app could never make.
 */
class InMemorySpaceRepositoryTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)

    /** Ana owns Flat 4B and Ben has joined it as a plain member. Ana is signed in. */
    private suspend fun teamOfTwo(): String {
        auth.signIn("ana@orbit.app", "secret")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember(id, Member("ben@orbit.app", "Ben", photoUrl = null, role = Role.MEMBER))
        return id
    }

    private suspend fun roleOf(spaceId: String, userId: String) =
        spaces.observeMembers(spaceId).first().single { it.userId == userId }.role

    @Test
    fun theOwnerChangesTheRoleOfAMember() = runTest {
        val id = teamOfTwo()

        spaces.changeRole(id, "ben@orbit.app", Role.ADMIN)
        assertEquals(Role.ADMIN, roleOf(id, "ben@orbit.app"))

        spaces.changeRole(id, "ben@orbit.app", Role.MEMBER)
        assertEquals(Role.MEMBER, roleOf(id, "ben@orbit.app"))
        assertEquals(Role.OWNER, roleOf(id, "ana@orbit.app"))
    }

    @Test
    fun anAdminChangesTheRoleOfAMember() = runTest {
        val id = teamOfTwo()
        spaces.addMember(id, Member("cy@orbit.app", "Cy", photoUrl = null, role = Role.MEMBER))
        spaces.changeRole(id, "ben@orbit.app", Role.ADMIN)
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")

        spaces.changeRole(id, "cy@orbit.app", Role.ADMIN)

        assertEquals(Role.ADMIN, roleOf(id, "cy@orbit.app"))
    }

    @Test
    fun aMemberAddedToASpaceIsInItsMemberIds() = runTest {
        val id = teamOfTwo()

        val space = spaces.observeSpaces().first().single { it.id == id }

        assertEquals(listOf("ana@orbit.app", "ben@orbit.app"), space.memberIds)
    }

    @Test
    fun nobodyBecomesOwnerThroughARoleChange() = runTest {
        val id = teamOfTwo()

        assertFailsWith<IllegalArgumentException> { spaces.changeRole(id, "ben@orbit.app", Role.OWNER) }
        assertEquals(Role.MEMBER, roleOf(id, "ben@orbit.app"))
    }

    @Test
    fun theRoleOfTheOwnerNeverChanges() = runTest {
        val id = teamOfTwo()

        assertFailsWith<IllegalStateException> { spaces.changeRole(id, "ana@orbit.app", Role.MEMBER) }
        assertEquals(Role.OWNER, roleOf(id, "ana@orbit.app"))
    }

    @Test
    fun aPlainMemberCannotChangeARole() = runTest {
        val id = teamOfTwo()
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")

        assertFailsWith<IllegalStateException> { spaces.changeRole(id, "ben@orbit.app", Role.ADMIN) }
        assertEquals(Role.MEMBER, roleOf(id, "ben@orbit.app"))
    }

    @Test
    fun changingTheRoleOfSomeoneWhoIsNotAMemberFails() = runTest {
        val id = teamOfTwo()

        assertFailsWith<IllegalStateException> { spaces.changeRole(id, "zoe@orbit.app", Role.ADMIN) }
    }

    @Test
    fun changingARoleNeedsASignedInUser() = runTest {
        val id = teamOfTwo()
        auth.signOut()

        assertFailsWith<IllegalStateException> { spaces.changeRole(id, "ben@orbit.app", Role.ADMIN) }
    }
}
