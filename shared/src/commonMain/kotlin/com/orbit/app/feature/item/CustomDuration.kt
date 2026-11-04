package com.orbit.app.feature.item

import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.duration_unit_days
import orbit.shared.generated.resources.duration_unit_hours
import orbit.shared.generated.resources.duration_unit_minutes
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class DurationUnit(val label: StringResource, val length: Duration) {
    MINUTES(Res.string.duration_unit_minutes, 1.minutes),
    HOURS(Res.string.duration_unit_hours, 1.hours),
    DAYS(Res.string.duration_unit_days, 1.days),
}

/** A length of time picked on the wheels, such as how long before to remind: none while [amount] is 0. */
data class CustomDuration(val amount: Int = 2, val unit: DurationUnit = DurationUnit.HOURS) {
    val length: Duration? get() = amount.takeIf { it > 0 }?.let { unit.length * it }
}
