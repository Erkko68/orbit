package com.orbit.app.data.repository

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.testing.InMemoryAuthRepository
import com.orbit.app.testing.InMemorySpaceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Taking a member out of a space (#264): who may remove whom, and that the member list and `memberIds`
 * move together. [com.orbit.app.data.repository.FirestoreSpaceRepository] itself calls the live SDK and
 * cannot run in a host test, so firestore.rules carries the same table and `firebase/tests/removing.test.js`
 * proves it there.
 */
class RemoveMemberTest {
    private val auth = InMemoryAuthRepository()
    private val spaces = InMemorySpaceRepository(auth)

    /** Ana owns Flat 4B, Ben is an admin of it and Cy a plain member. */
    private suspend fun flat(signedInAs: String = "ana@orbit.app"): String {
        auth.register("ana@orbit.app", "secret", displayName = "Ana")
        val id = spaces.createSpace("Flat 4B", accent = 0)
        spaces.addMember(id, Member("ben@orbit.app", "Ben", photoUrl = null, role = Role.ADMIN))
        spaces.addMember(id, Member("cy@orbit.app", "Cy", photoUrl = null, role = Role.MEMBER))
        if (signedInAs != "ana@orbit.app") {
            auth.signOut()
            auth.signIn(signedInAs, "secret")
        }
        return id
    }

    private suspend fun memberIds(id: String) = spaces.observeMembers(id).first().map { it.userId }

    @Test
    fun theOwnerTakesAnAdminAndAPlainMemberOut() = runTest {
        val id = flat()

        spaces.removeMember(id, "ben@orbit.app", "Ben")
        spaces.removeMember(id, "cy@orbit.app", "Cy")

        assertEquals(listOf("ana@orbit.app"), memberIds(id))
    }

    @Test
    fun theSpaceForgetsThemButKeepsTheirName() = runTest {
        val id = flat()

        spaces.removeMember(id, "cy@orbit.app", "Cy")

        val space = spaces.observeSpaces().first().single { it.id == id }
        assertEquals(listOf("ana@orbit.app", "ben@orbit.app"), space.memberIds)
        assertEquals("Cy", space.formerMembers["cy@orbit.app"])
    }

    @Test
    fun anAdminTakesOutAPlainMemberButNotAnotherAdmin() = runTest {
        val id = flat(signedInAs = "ben@orbit.app")
        spaces.addMember(id, Member("dana@orbit.app", "Dana", photoUrl = null, role = Role.ADMIN))

        spaces.removeMember(id, "cy@orbit.app", "Cy")
        assertFailsWith<IllegalStateException> { spaces.removeMember(id, "dana@orbit.app", "Dana") }

        assertTrue("dana@orbit.app" in memberIds(id))
    }

    @Test
    fun theOwnerIsNeverRemoved() = runTest {
        val id = flat(signedInAs = "ben@orbit.app")

        assertFailsWith<IllegalStateException> { spaces.removeMember(id, "ana@orbit.app", "Ana") }

        assertTrue("ana@orbit.app" in memberIds(id))
    }

    @Test
    fun aPlainMemberRemovesNobody() = runTest {
        val id = flat(signedInAs = "cy@orbit.app")

        assertFailsWith<IllegalStateException> { spaces.removeMember(id, "ben@orbit.app", "Ben") }

        assertEquals(3, memberIds(id).size)
    }

    @Test
    fun removingYourselfIsLeaving() = runTest {
        val id = flat(signedInAs = "ben@orbit.app")

        assertFailsWith<IllegalStateException> { spaces.removeMember(id, "ben@orbit.app", "Ben") }

        assertTrue("ben@orbit.app" in memberIds(id))
    }
}
