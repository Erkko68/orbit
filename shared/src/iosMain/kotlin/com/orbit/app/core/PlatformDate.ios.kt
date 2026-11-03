package com.orbit.app.core

import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneForSecondsFromGMT

internal actual fun formatDate(date: LocalDate, languageTag: String): String {
    val utc = NSTimeZone.timeZoneForSecondsFromGMT(0)
    val calendar = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian).apply {
        timeZone = utc
    }
    val components = NSDateComponents().apply {
        year = date.year.toLong()
        month = date.month.ordinal.toLong() + 1
        day = date.day.toLong()
    }
    val value = requireNotNull(calendar.dateFromComponents(components))
    return NSDateFormatter().apply {
        locale = NSLocale(localeIdentifier = languageTag)
        this.calendar = calendar
        timeZone = utc
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }.stringFromDate(value)
}
