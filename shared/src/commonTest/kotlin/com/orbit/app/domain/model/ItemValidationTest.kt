package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ItemValidationTest {

    private val members = listOf("ana", "ben")
    private val friday = LocalDate(2026, 10, 9)
    private val sunday = LocalDate(2026, 10, 11)

    private fun errors(title: String = "Buy milk", modules: ItemModules = ItemModules()) =
        validateItem(title, modules, members)

    @Test
    fun validItemHasNoErrors() {
        val modules = ItemModules(
            schedule = Schedule(friday, time = LocalTime(20, 0), duration = 30.minutes),
            assignment = Assignment.ToMember("ana"),
            checklist = Checklist(listOf(ChecklistEntry("1", "Milk", quantity = 2))),
            reminder = Reminder(listOf(Duration.ZERO)),
        )
        assertEquals(emptyList(), errors(modules = modules))
    }

    @Test
    fun titleIsTrimmedBeforeChecking() {
        assertEquals(listOf(ItemError.BlankTitle), errors(title = "   "))
        assertEquals(emptyList(), errors(title = " " + "a".repeat(120) + " "))
        assertEquals(listOf(ItemError.TitleTooLong), errors(title = "a".repeat(121)))
    }

    @Test
    fun scheduleIsAPointOrAWindow() {
        assertEquals(emptyList(), errors(modules = ItemModules(Schedule(friday, endDate = sunday))))
        assertEquals(
            listOf(ItemError.TimeWithEndDate),
            errors(modules = ItemModules(Schedule(friday, time = LocalTime(20, 0), endDate = sunday))),
        )
        assertEquals(
            listOf(ItemError.EndNotAfterStart),
            errors(modules = ItemModules(Schedule(friday, endDate = friday))),
        )
        assertEquals(
            listOf(ItemError.NonPositiveDuration),
            errors(modules = ItemModules(Schedule(friday, duration = Duration.ZERO))),
        )
    }

    @Test
    fun assigneeMustBeAMember() {
        assertEquals(emptyList(), errors(modules = ItemModules(assignment = Assignment.Anyone)))
        assertEquals(emptyList(), errors(modules = ItemModules(assignment = Assignment.Claim())))
        assertEquals(
            listOf(ItemError.AssigneeNotMember),
            errors(modules = ItemModules(assignment = Assignment.ToMember("zoe"))),
        )
        assertEquals(
            listOf(ItemError.AssigneeNotMember),
            errors(modules = ItemModules(assignment = Assignment.Claim("zoe"))),
        )
    }

    @Test
    fun checklistErrorsNameTheEntry() {
        val checklist = Checklist(
            listOf(
                ChecklistEntry("1", "Milk"),
                ChecklistEntry("2", " "),
                ChecklistEntry("3", "Eggs", quantity = 0),
            ),
        )
        assertEquals(
            listOf(ItemError.BlankChecklistEntry("2"), ItemError.NonPositiveQuantity("3")),
            errors(modules = ItemModules(checklist = checklist)),
        )
    }

    @Test
    fun reminderNeedsAScheduleAndNonNegativeOffsets() {
        assertEquals(
            listOf(ItemError.ReminderWithoutSchedule),
            errors(modules = ItemModules(reminder = Reminder(listOf(10.minutes)))),
        )
        assertEquals(
            listOf(ItemError.NegativeReminderOffset),
            errors(modules = ItemModules(Schedule(friday), reminder = Reminder(listOf((-5).minutes)))),
        )
    }

    @Test
    fun modulesAreAddedRemovedAndListedInCardOrder() {
        val reminder = Reminder(listOf(10.minutes))
        val schedule = Schedule(friday, endDate = sunday)
        val modules = ItemModules().with(reminder).with(Assignment.Anyone).with(schedule)

        assertEquals(listOf(schedule, Assignment.Anyone, reminder), modules.all)
        assertEquals(schedule, modules[ModuleType.SCHEDULE])
        assertEquals(sunday, schedule.deadline)
        assertEquals(friday, Schedule(friday).deadline)

        val without = modules.without(ModuleType.ASSIGNMENT)
        assertNull(without[ModuleType.ASSIGNMENT])
        assertEquals(listOf(ModuleType.SCHEDULE, ModuleType.REMINDER), without.all.map { it.type })
    }

    @Test
    fun everyModuleTypeCanBeRemoved() {
        val full = ItemModules()
            .with(Schedule(friday))
            .with(Assignment.Anyone)
            .with(Checklist(emptyList()))
            .with(Reminder(emptyList()))
        assertEquals(ModuleType.entries, full.all.map { it.type })

        ModuleType.entries.forEach { type ->
            assertEquals(ModuleType.entries - type, full.without(type).all.map { it.type })
        }
    }
}
