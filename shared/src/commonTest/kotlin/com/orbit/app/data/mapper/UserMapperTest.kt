package com.orbit.app.data.mapper

import com.orbit.app.core.deviceLanguage
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserMapperTest {

    @Test
    fun mapsUserWithAllFieldsPresent() {
        val user = mapUser(
            id = "user-123",
            displayName = "Alice Wonderland",
            photoUrl = "https://example.com/alice.png",
        )

        assertEquals("user-123", user.id)
        assertEquals("Alice Wonderland", user.displayName)
        assertEquals("https://example.com/alice.png", user.photoUrl)
    }

    @Test
    fun mapsUserWithMissingFieldsFallingBackToDefaults() {
        val user = mapUser(
            id = "user-456",
            displayName = null,
            photoUrl = null,
            fallbackDisplayName = "Fallback Name",
            fallbackPhotoUrl = "https://example.com/fallback.png",
        )

        assertEquals("user-456", user.id)
        assertEquals("Fallback Name", user.displayName)
        assertEquals("https://example.com/fallback.png", user.photoUrl)
    }

    @Test
    fun mapsUserWithBlankDisplayNameFallingBackToFallback() {
        val user = mapUser(
            id = "user-abc",
            displayName = "   ",
            photoUrl = null,
            fallbackDisplayName = "bob@orbit.app",
        )

        assertEquals("bob@orbit.app", user.displayName)
    }

    @Test
    fun buildsUserProfileDocument() {
        val doc = userProfileDocument(
            displayName = "Charlie",
            photoUrl = "https://example.com/charlie.png",
            language = "es",
            timeZone = TimeZone.of("UTC"),
        )

        assertEquals("Charlie", doc["displayName"])
        assertEquals("https://example.com/charlie.png", doc["photoUrl"])
        assertEquals("es", doc["language"])
        assertEquals("UTC", doc["timeZone"])
    }

    @Test
    fun buildsUserProfileDocumentWithoutPhotoUrlWhenNull() {
        val doc = userProfileDocument(
            displayName = "David",
            photoUrl = null,
        )

        assertEquals("David", doc["displayName"])
        assertNull(doc["photoUrl"])
        assertEquals(deviceLanguage(), doc["language"])
        assertEquals(TimeZone.currentSystemDefault().id, doc["timeZone"])
    }

    @Test
    fun buildsDevicePreferencesDocument() {
        val doc = devicePreferencesDocument(
            language = "ca",
            timeZone = TimeZone.of("Europe/Madrid"),
        )

        assertEquals("ca", doc["language"])
        assertEquals("Europe/Madrid", doc["timeZone"])
    }

    @Test
    fun buildsDevicePreferencesDocumentWithDefaults() {
        val doc = devicePreferencesDocument()

        assertEquals(deviceLanguage(), doc["language"])
        assertEquals(TimeZone.currentSystemDefault().id, doc["timeZone"])
    }
}
