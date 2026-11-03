package com.orbit.app.core

import kotlinx.datetime.LocalDate
import java.text.DateFormat
import java.util.GregorianCalendar
import java.util.Locale

// The calendar and the format share the default time zone, so the day never shifts.
internal actual fun formatDate(date: LocalDate, languageTag: String): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag(languageTag))
        .format(GregorianCalendar(date.year, date.month.ordinal, date.day).time)
