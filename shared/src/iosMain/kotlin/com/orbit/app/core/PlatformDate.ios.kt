package com.orbit.app.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSLocale

// The calendar and the formatter share the device's time zone, so the day never shifts.
internal actual fun formatDate(date: LocalDate, languageTag: String): String {
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
    return NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = languageTag)
        this.calendar = calendar
        dateStyle = NSDateFormatterMediumStyle
    }.stringFromDate(requireNotNull(calendar.dateFromComponents(date.toNSDateComponents())))
}
