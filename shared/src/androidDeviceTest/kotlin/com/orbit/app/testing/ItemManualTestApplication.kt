package com.orbit.app.testing

import android.app.Application
import kotlinx.coroutines.runBlocking

/** Initializes fixtures only for the manual Activity, leaving ordinary instrumentation tests alone. */
class ItemManualTestApplication : Application() {
    private var prepared = false

    fun prepareFixtures() {
        if (prepared) return
        // These test repositories only change memory; there is no network work on the main thread.
        val fixtures = runBlocking { createItemManualTestFixtures() }
        startTestKoin(fixtures.auth, fixtures.spaces, fixtures.items)
        prepared = true
    }
}
