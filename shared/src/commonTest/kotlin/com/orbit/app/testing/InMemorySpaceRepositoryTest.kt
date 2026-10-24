package com.orbit.app.testing

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The in-memory repository refuses a role change, a transfer or leaving as the security rules do, so a test
 * cannot rely on a change the app could never make.
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

    private suspend fun spaceOf(spaceId: String) = spaces.observeSpaces().first().single { it.id == spaceId }

    @Test
    fun theOwnerChangesTheNameAndTheColour() = runTest {
        val id = teamOfTwo()

        spaces.updateSpace(id, name = "Flat 4C", accent = 3)

        assertEquals("Flat 4C", spaceOf(id).name)
        assertEquals(3, spaceOf(id).accent)
    }

    @Test
    fun anAdminChangesOnlyTheFieldGiven() = runTest {
        val id = teamOfTwo()
        spaces.changeRole(id, "ben@orbit.app", Role.ADMIN)
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")

        spaces.updateSpace(id, name = null, accent = 2)
        assertEquals("Flat 4B", spaceOf(id).name)
        assertEquals(2, spaceOf(id).accent)

        spaces.updateSpace(id, name = "Flat 4D", accent = null)
        assertEquals("Flat 4D", spaceOf(id).name)
        assertEquals(2, spaceOf(id).accent)
    }

    @Test
    fun aPlainMemberCannotEditTheSpace() = runTest {
        val id = teamOfTwo()
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")

        assertFailsWith<IllegalStateException> { spaces.updateSpace(id, name = "Mine now", accent = null) }
        assertEquals("Flat 4B", spaceOf(id).name)
    }

    @Test
    fun refusesABlankNameAndAnEditThatChangesNothing() = runTest {
        val id = teamOfTwo()

        assertFailsWith<IllegalArgumentException> { spaces.updateSpace(id, name = "   ", accent = null) }
        assertFailsWith<IllegalArgumentException> { spaces.updateSpace(id, name = null, accent = null) }
        assertEquals("Flat 4B", spaceOf(id).name)
    }

    @Test
    fun editingASpaceNeedsASignedInUser() = runTest {
        val id = teamOfTwo()
        auth.signOut()

        assertFailsWith<IllegalStateException> { spaces.updateSpace(id, name = "Flat 4C", accent = null) }
    }

    @Test
    fun everyoneInTheSpaceSeesTheChange() = runTest {
        val id = teamOfTwo()
        spaces.updateSpace(id, name = "Flat 4C", accent = null)
        auth.signOut()
        auth.signIn("ben@orbit.app", "secret")

        assertEquals("Flat 4C", spaceOf(id).name)
    }

    private suspend fun signInAs(userId: String) {
        auth.signOut()
        auth.signIn(userId, "secret")
    }

    @Test
    fun aMemberLeavesTheSpace() = runTest {
        val id = teamOfTwo()
        signInAs("ben@orbit.app")

        spaces.leaveSpace(id)

        assertEquals(emptyList(), spaces.observeSpaces().first())
        signInAs("ana@orbit.app")
        assertEquals(listOf("ana@orbit.app"), spaceOf(id).memberIds)
        assertEquals(listOf("ana@orbit.app"), spaces.observeMembers(id).first().map { it.userId })
    }

    @Test
    fun theOwnerCannotLeave() = runTest {
        val id = teamOfTwo()

        assertFailsWith<IllegalStateException> { spaces.leaveSpace(id) }
        assertEquals(Role.OWNER, roleOf(id, "ana@orbit.app"))
    }

    @Test
    fun theOwnerDeletesTheSpaceForEveryone() = runTest {
        val id = teamOfTwo()

        spaces.deleteSpace(id)

        assertEquals(emptyList(), spaces.observeSpaces().first())
        signInAs("ben@orbit.app")
        assertEquals(emptyList(), spaces.observeSpaces().first())
    }

    @Test
    fun onlyTheOwnerDeletesTheSpace() = runTest {
        val id = teamOfTwo()
        signInAs("ben@orbit.app")

        assertFailsWith<IllegalStateException> { spaces.deleteSpace(id) }
        assertEquals(listOf(id), spaces.observeSpaces().first().map { it.id })
    }

    @Test
    fun theOwnerHandsTheSpaceOverAndBecomesAnAdmin() = runTest {
        val id = teamOfTwo()

        spaces.transferOwnership(id, "ben@orbit.app")

        assertEquals(Role.OWNER, roleOf(id, "ben@orbit.app"))
        assertEquals(Role.ADMIN, roleOf(id, "ana@orbit.app"))
        spaces.leaveSpace(id)
        assertEquals(emptyList(), spaces.observeSpaces().first())
    }

    @Test
    fun onlyTheOwnerHandsTheSpaceOverAndOnlyToAnotherMember() = runTest {
        val id = teamOfTwo()
        assertFailsWith<IllegalStateException> { spaces.transferOwnership(id, "ana@orbit.app") }
        assertFailsWith<IllegalStateException> { spaces.transferOwnership(id, "zoe@orbit.app") }

        signInAs("ben@orbit.app")
        assertFailsWith<IllegalStateException> { spaces.transferOwnership(id, "ben@orbit.app") }
        assertEquals(Role.OWNER, roleOf(id, "ana@orbit.app"))
    }
}
