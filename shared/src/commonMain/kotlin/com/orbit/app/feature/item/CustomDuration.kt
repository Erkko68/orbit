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

private const val AMOUNT_DIGITS = 3

enum class DurationUnit(val label: StringResource, val length: Duration) {
    MINUTES(Res.string.duration_unit_minutes, 1.minutes),
    HOURS(Res.string.duration_unit_hours, 1.hours),
    DAYS(Res.string.duration_unit_days, 1.days),
}

/** A length of time typed by hand, such as how long before to remind or how long something takes. */
data class CustomDuration(val amount: String = "", val unit: DurationUnit = DurationUnit.MINUTES) {
    val length: Duration? get() = amount.toIntOrNull()?.takeIf { it > 0 }?.let { unit.length * it }

    fun withAmount(text: String) = copy(amount = text.filter(Char::isDigit).take(AMOUNT_DIGITS))
}
