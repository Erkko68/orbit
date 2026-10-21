package com.orbit.app.data.mapper

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.ModuleType
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Preset
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class ItemMapperTest {
    private val createdAt = Instant.fromEpochSeconds(1_760_000_000)

    private val stored = StoredItem(title = "Buy milk", status = "open", createdBy = "ana", createdAt = createdAt)

    private val modules = ItemModules(
        schedule = Schedule(LocalDate(2026, 10, 9), time = LocalTime(20, 0), duration = 90.minutes),
        assignment = Assignment.ToMember("ben"),
        checklist = Checklist(
            listOf(ChecklistEntry("e2", "Milk", quantity = 2, order = 0), ChecklistEntry("e1", "Bread", true, order = 1)),
        ),
        reminder = Reminder(listOf(30.minutes)),
    )

    /** What a document written for [modules] reads back as. */
    private fun storedModules(modules: ItemModules): StoredItem {
        val document = itemDocument("space-1", NewItem("Buy milk", modules = modules), "ana").modules
        return stored.copy(
            moduleKeys = modules.all.map { moduleKey(it.type) }.toSet(),
            modules = document,
        )
    }

    @Test
    fun aNewItemIsOpenAndMadeByItsCreator() {
        val document = itemDocument("space-1", NewItem("Buy milk", "Semi-skimmed", Preset.SHOPPING_LIST), "ana")

        assertEquals("space-1", document.spaceId)
        assertEquals("Buy milk", document.title)
        assertEquals("Semi-skimmed", document.description)
        assertEquals("open", document.status)
        assertEquals("shopping_list", document.preset)
        assertEquals("ana", document.createdBy)
        assertEquals(Timestamp.ServerTimestamp, document.createdAt)
        assertNull(document.modules.schedule)
    }

    @Test
    fun writesEachModuleInTheStoredShape() {
        val document = itemDocument("space-1", NewItem("Buy milk", modules = modules), "ana").modules

        val schedule = document.schedule!!
        assertEquals("2026-10-09", schedule.date)
        assertEquals("20:00", schedule.time)
        assertNull(schedule.endDate)
        assertEquals(90, schedule.durationMinutes)
        assertEquals("member", document.assignment!!.mode)
        assertEquals("ben", document.assignment.assigneeId)
        val milk = document.checklist!!.entries.getValue("e2")
        assertEquals(listOf("Milk", false, 2, 0), listOf(milk.text, milk.checked, milk.quantity, milk.order))
        assertEquals(1, document.checklist.entries.getValue("e1").order)
        assertEquals(listOf(30), document.reminder!!.offsetsMinutes)
    }

    @Test
    fun everyModuleReadsBackAsWritten() {
        val item = mapItem("item-1", storedModules(modules))

        assertEquals(modules, item?.modules)
    }

    @Test
    fun everyAssignmentModeReadsBackAsWritten() {
        val assignments =
            listOf(Assignment.ToMember("ben"), Assignment.Anyone, Assignment.Claim(), Assignment.Claim("ben"))

        val read = assignments.map {
            mapItem("item-1", storedModules(ItemModules(assignment = it)))?.modules?.assignment
        }

        assertEquals(assignments, read)
    }

    @Test
    fun aWindowReadsBackWithItsEndDate() {
        val window = ItemModules(schedule = Schedule(LocalDate(2026, 10, 9), endDate = LocalDate(2026, 10, 12)))

        assertEquals(window, mapItem("item-1", storedModules(window))?.modules)
    }

    @Test
    fun mapsTheCoreFields() {
        val item = mapItem("item-1", stored.copy(description = "Semi-skimmed", preset = "event"))

        assertEquals(
            Item("item-1", "Buy milk", "Semi-skimmed", ItemStatus.Open, Preset.EVENT, "ana", createdAt),
            item,
        )
    }

    @Test
    fun aDoneItemCarriesWhoCompletedItAndWhen() {
        val doneAt = Instant.fromEpochSeconds(1_760_000_600)

        val item = mapItem("item-1", stored.copy(status = "done", completedBy = "ben", completedAt = doneAt))

        assertEquals(ItemStatus.Done("ben", doneAt), item?.status)
    }

    @Test
    fun aDoneWithoutItsCompleterOrAnUnknownStatusReadsAsOpen() {
        val statuses =
            listOf(stored.copy(status = "done"), stored.copy(status = "archived"), stored.copy(status = null))

        assertEquals(List(3) { ItemStatus.Open }, statuses.map { mapItem("item-1", it)?.status })
    }

    @Test
    fun skipsAnItemWithoutATitleCreatorOrCreationTime() {
        val malformed = listOf(stored.copy(title = null), stored.copy(createdBy = null), stored.copy(createdAt = null))

        assertEquals(listOf(null, null, null), malformed.map { mapItem("item-1", it) })
    }

    @Test
    fun whatANewerBuildWroteIsSkippedAndTheRestStillReads() {
        val newer = stored.copy(
            preset = "chore",
            moduleKeys = setOf("schedule", "assignment", "cost"),
            modules = ModulesDocument(
                schedule = ScheduleDocument("2026-10-09"),
                assignment = AssignmentDocument("rotation", "ben"),
            ),
        )

        val item = mapItem("item-1", newer)

        assertNull(item?.preset)
        assertEquals(ItemModules(schedule = Schedule(LocalDate(2026, 10, 9))), item?.modules)
    }

    @Test
    fun skipsAMalformedModuleAndKeepsTheOthers() {
        val broken = stored.copy(
            moduleKeys = setOf(moduleKey(ModuleType.SCHEDULE), moduleKey(ModuleType.ASSIGNMENT)),
            modules = ModulesDocument(
                schedule = ScheduleDocument("next friday"),
                assignment = AssignmentDocument("member"),
            ),
        )

        assertEquals(ItemModules(), mapItem("item-1", broken)?.modules)
    }

    @Test
    fun aTickOnARemovedEntryIsSkippedAndEntriesFollowTheirOrder() {
        val checklist = ChecklistDocument(
            mapOf(
                "late" to EntryDocument("Bread", checked = null, order = null),
                "ticked" to EntryDocument(text = null, checked = true, order = null),
                "first" to EntryDocument("Milk", checked = true, order = 0),
            ),
        )

        val item = mapItem("item-1", stored.copy(modules = ModulesDocument(checklist = checklist)))

        assertEquals(
            Checklist(listOf(ChecklistEntry("first", "Milk", checked = true, order = 0), ChecklistEntry("late", "Bread"))),
            item?.modules?.checklist,
        )
    }

    @Test
    fun entriesWithTheSameOrderHaveAStableIdOrder() {
        val checklist = ChecklistDocument(
            mapOf(
                "z" to EntryDocument("Last", checked = false, order = 2),
                "a" to EntryDocument("First", checked = false, order = 2),
            ),
        )
        val item = mapItem("item-1", stored.copy(modules = ModulesDocument(checklist = checklist)))

        assertEquals(listOf("a", "z"), item?.modules?.checklist?.entries?.map { it.id })
    }
}
