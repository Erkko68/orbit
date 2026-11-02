package com.orbit.app.core

import kotlinx.datetime.LocalDate
import java.text.DateFormat
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

internal actual fun formatDate(date: LocalDate, languageTag: String): String {
    val locale = Locale.forLanguageTag(languageTag)
    val utc = TimeZone.getTimeZone("UTC")
    val calendar = GregorianCalendar(utc, locale).apply {
        clear()
        set(date.year, date.month.ordinal, date.day)
    }
    return DateFormat.getDateInstance(DateFormat.MEDIUM, locale).apply {
        timeZone = utc
    }.format(calendar.time)
}
