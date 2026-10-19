package com.orbit.app.domain.repository

/** The space the user opened last, kept on this device so the app can open into it. */
interface LastSpaceRepository {
    /** The id of the space opened last, or null if there is none or it was forgotten. */
    fun lastSpaceId(): String?

    fun remember(spaceId: String)

    fun forget()
}
