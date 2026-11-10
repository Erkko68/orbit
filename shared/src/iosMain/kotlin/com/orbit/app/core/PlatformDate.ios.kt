package com.orbit.app.core

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSLocale

// The calendar and the formatter share the device's time zone, so the day never shifts.
internal actual fun formatDate(date: LocalDate, languageTag: String): String =
    formatter(languageTag).apply { dateStyle = NSDateFormatterMediumStyle }.format(date)

internal actual fun formatMonth(date: LocalDate, languageTag: String): String =
    formatter(languageTag).apply { setLocalizedDateFormatFromTemplate("LLLLyyyy") }.format(date)

// The symbols start on Sunday.
internal actual fun weekdayInitials(languageTag: String): List<String> {
    val symbols = formatter(languageTag).veryShortStandaloneWeekdaySymbols.map { it as String }
    return symbols.drop(1) + symbols.first()
}

// NSCalendar counts from Sunday = 1.
internal actual fun firstDayOfWeek(languageTag: String): DayOfWeek {
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply {
        locale = NSLocale(localeIdentifier = languageTag)
    }
    return DayOfWeek((calendar.firstWeekday.toInt() + 5) % 7 + 1)
}

private fun formatter(languageTag: String) = NSDateFormatter().apply {
    locale = NSLocale(localeIdentifier = languageTag)
    calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
}

private fun NSDateFormatter.format(date: LocalDate): String =
    stringFromDate(requireNotNull(calendar.dateFromComponents(date.toNSDateComponents())))
