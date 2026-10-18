package com.orbit.app.data.repository

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsLastSpaceRepositoryTest {
    private val settings = MapSettings()
    private val repository = SettingsLastSpaceRepository(settings)

    @Test
    fun hasNoLastSpaceAtFirst() {
        assertNull(repository.lastSpaceId())
    }

    @Test
    fun remembersTheLastSpaceInTheSettings() {
        repository.remember("space-1")

        // A new instance over the same settings, as after the app restarts.
        assertEquals("space-1", SettingsLastSpaceRepository(settings).lastSpaceId())
    }

    @Test
    fun theNextSpaceReplacesIt() {
        repository.remember("space-1")
        repository.remember("space-2")

        assertEquals("space-2", repository.lastSpaceId())
    }

    @Test
    fun forgetsIt() {
        repository.remember("space-1")

        repository.forget()

        assertNull(repository.lastSpaceId())
    }
}
