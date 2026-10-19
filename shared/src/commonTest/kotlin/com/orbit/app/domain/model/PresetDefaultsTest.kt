package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class PresetDefaultsTest {
    private val today = LocalDate(2026, 10, 19)

    @Test
    fun aTaskIsDueTodayAndOpenToAnyone() {
        assertEquals(
            ItemModules(schedule = Schedule(today), assignment = Assignment.Anyone),
            Preset.TASK.defaultModules(today),
        )
    }

    @Test
    fun aShoppingListStartsWithAnEmptyChecklist() {
        assertEquals(ItemModules(checklist = Checklist(emptyList())), Preset.SHOPPING_LIST.defaultModules(today))
    }

    @Test
    fun anEventIsTodayAndRemindsAtItsTime() {
        assertEquals(
            ItemModules(schedule = Schedule(today), reminder = Reminder(listOf(Duration.ZERO))),
            Preset.EVENT.defaultModules(today),
        )
    }

    @Test
    fun everyPresetStartsValid() {
        for (preset in Preset.entries) {
            assertEquals(emptyList(), validateItem("Title", preset.defaultModules(today), emptyList()), "$preset")
        }
    }

    @Test
    fun eachDefaultModuleHasItsType() {
        for (type in ModuleType.entries) {
            assertEquals(type, defaultModule(type, today).type)
        }
    }
}
