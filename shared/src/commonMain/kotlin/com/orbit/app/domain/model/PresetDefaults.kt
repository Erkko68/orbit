package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Duration

/** The smallest valid module of [type], which the user then edits. Schedules start on [today]. */
fun defaultModule(type: ModuleType, today: LocalDate): Module = when (type) {
    ModuleType.SCHEDULE -> Schedule(date = today)
    ModuleType.ASSIGNMENT -> Assignment.Anyone
    ModuleType.CHECKLIST -> Checklist(entries = emptyList())
    ModuleType.REMINDER -> Reminder(offsets = listOf(Duration.ZERO))
}

/** The modules an item of this preset starts with: a task is due and open to anyone, an event reminds. */
fun Preset.defaultModules(today: LocalDate): ItemModules = when (this) {
    Preset.TASK -> listOf(ModuleType.SCHEDULE, ModuleType.ASSIGNMENT)
    Preset.SHOPPING_LIST -> listOf(ModuleType.CHECKLIST)
    Preset.EVENT -> listOf(ModuleType.SCHEDULE, ModuleType.REMINDER)
}.fold(ItemModules()) { modules, type -> modules.with(defaultModule(type, today)) }
