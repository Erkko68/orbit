package com.orbit.app.data.mapper

import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpaceMapperTest {
    @Test
    fun mapsASpaceFromItsFields() {
        val space = mapSpace("space-1", name = "Flat 4B", accent = 2, memberIds = listOf("ana", "ben"))

        assertEquals(Space("space-1", "Flat 4B", accent = 2, memberIds = listOf("ana", "ben")), space)
    }

    @Test
    fun fillsInWhatAnOlderOrOfflineSpaceLacks() {
        val space = mapSpace("space-1", name = "Flat 4B", accent = null, memberIds = null)

        assertEquals(Space("space-1", "Flat 4B", accent = 0, memberIds = emptyList()), space)
    }

    @Test
    fun skipsASpaceWithoutAName() {
        assertNull(mapSpace("space-1", name = null, accent = 1, memberIds = listOf("ana")))
    }

    @Test
    fun mapsAMemberFromItsFields() {
        val photo = "https://orbit.app/ana.png"
        val member = mapMember("ana", displayName = "Ana López", photoUrl = photo, role = "admin")

        assertEquals(Member("ana", "Ana López", photo, Role.ADMIN), member)
    }

    @Test
    fun mapsEveryRoleAndGivesAnUnknownOneTheLeastPrivilege() {
        val stored = listOf("owner", "admin", "member", "moderator", null)

        val roles = stored.map { mapMember("ana", "Ana", photoUrl = null, role = it).role }

        assertEquals(listOf(Role.OWNER, Role.ADMIN, Role.MEMBER, Role.MEMBER, Role.MEMBER), roles)
    }

    @Test
    fun aMemberWithoutAProfileCopyHasNoNameAndNoPhoto() {
        val member = mapMember("ana", displayName = null, photoUrl = null, role = "member")

        assertEquals("", member.displayName)
        assertNull(member.photoUrl)
    }

    @Test
    fun aNewSpaceHasItsCreatorAsTheOnlyMember() {
        val document = spaceDocument("Flat 4B", accent = 3, creatorId = "ana")

        assertEquals("Flat 4B", document.name)
        assertEquals(3, document.accent)
        assertEquals(listOf("ana"), document.memberIds)
        assertEquals("ana", document.createdBy)
        assertEquals(Timestamp.ServerTimestamp, document.createdAt)
    }

    @Test
    fun theCreatorsMembershipIsTheOwnerWithTheirProfileCopy() {
        val withPhoto = ownerMemberDocument("Ana", "https://orbit.app/ana.png")
        val withoutPhoto = ownerMemberDocument("Ana", null)

        assertEquals("owner", withPhoto.role)
        assertEquals("Ana", withPhoto.displayName)
        assertEquals("https://orbit.app/ana.png", withPhoto.photoUrl)
        assertEquals(Timestamp.ServerTimestamp, withPhoto.joinedAt)
        assertNull(withoutPhoto.photoUrl)
        assertEquals("owner", withoutPhoto.role)
    }
}
