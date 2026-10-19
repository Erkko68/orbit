package com.orbit.app.data.repository

import com.orbit.app.domain.repository.LastSpaceRepository
import com.russhwolf.settings.Settings

/** The last opened space in the device's settings (SharedPreferences on Android, UserDefaults on iOS). */
class SettingsLastSpaceRepository(private val settings: Settings) : LastSpaceRepository {
    override fun lastSpaceId(): String? = settings.getStringOrNull(KEY)

    override fun remember(spaceId: String) {
        settings.putString(KEY, spaceId)
    }

    override fun forget() {
        settings.remove(KEY)
    }

    private companion object {
        const val KEY = "last_space_id"
    }
}
