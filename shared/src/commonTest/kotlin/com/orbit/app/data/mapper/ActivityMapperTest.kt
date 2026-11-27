package com.orbit.app.data.mapper

import com.orbit.app.domain.model.ActivityAction
import com.orbit.app.domain.model.ActivityTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ActivityMapperTest {
    private val at = Instant.fromEpochSeconds(1_800_000_000)
    private val stored = StoredActivity(
        actorId = "ana",
        at = at,
        action = "edit",
        target = "expense",
        targetId = "e1",
        title = "Dinner",
        changedFields = setOf("title", "split"),
    )

    @Test
    fun readsAnEntry() {
        val entry = mapActivity("a1", stored)!!

        assertEquals("ana", entry.actorId)
        assertEquals(at, entry.at)
        assertEquals(ActivityAction.EDIT, entry.action)
        assertEquals(ActivityTarget.EXPENSE, entry.target)
        assertEquals("e1", entry.targetId)
        assertEquals(setOf("title", "split"), entry.changedFields)
        assertFalse(entry.byAssistant)
    }

    @Test
    fun readsTheAssistantsMark() {
        val entry = mapActivity("a1", stored.copy(source = SOURCE_ASSISTANT, planId = "p1"))!!

        assertTrue(entry.byAssistant)
        assertEquals("p1", entry.planId)
    }

    @Test
    fun skipsAnEntryMissingWhoWhenOrWhatOrWithAnUnknownAction() {
        assertNull(mapActivity("a1", stored.copy(actorId = null)))
        assertNull(mapActivity("a1", stored.copy(at = null)))
        assertNull(mapActivity("a1", stored.copy(targetId = null)))
        assertNull(mapActivity("a1", stored.copy(action = "archive")))
        assertNull(mapActivity("a1", stored.copy(target = "space")))
    }

    @Test
    fun writesAnEntryMarkingOnlyAPlansChanges() {
        val byHand = activityDocument("ana", ActivityAction.DELETE, ActivityTarget.ITEM, "i1", "Bins", mapOf("title" to "\"Bins\""))
        val byPlan = activityDocument("ana", ActivityAction.CREATE, ActivityTarget.ITEM, "i2", "Plants", planId = "p1")

        assertEquals("delete", byHand.action)
        assertEquals("item", byHand.target)
        assertEquals(mapOf("title" to "\"Bins\""), byHand.changes)
        assertNull(byHand.source)
        assertNull(byPlan.changes)
        assertEquals(SOURCE_ASSISTANT, byPlan.source)
        assertEquals("p1", byPlan.planId)
    }
}
