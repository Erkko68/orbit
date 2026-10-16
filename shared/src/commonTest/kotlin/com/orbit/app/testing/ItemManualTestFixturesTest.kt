package com.orbit.app.testing

import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.validateItem
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ItemManualTestFixturesTest {
    @Test
    fun providesASignedInSpaceAndValidTaskListAndEvent() = runTest {
        val fixtures = createItemManualTestFixtures()
        val user = assertNotNull(fixtures.auth.session.first())
        val space = fixtures.spaces.observeSpaces().first().single()
        assertEquals("Ana", user.displayName)
        assertTrue(user.id in space.memberIds)
        val items = fixtures.items.observeItems(space.id).first()
        assertEquals(setOf(Preset.TASK, Preset.SHOPPING_LIST, Preset.EVENT), items.map { it.preset }.toSet())
        assertTrue(items.single { it.preset == Preset.TASK }.modules.all.isEmpty())
        val entries = assertNotNull(items.single { it.preset == Preset.SHOPPING_LIST }.modules.checklist).entries
        assertEquals(6, entries.size)
        assertEquals(2, entries.count { it.checked })
        assertNotNull(items.single { it.preset == Preset.EVENT }.modules.schedule?.time)
        for (item in items) assertTrue(validateItem(item.title, item.modules, space.memberIds).isEmpty())
    }
}
