package com.orbit.app.core

import android.text.format.DateFormat as AndroidDateFormat
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

// The calendar and the format share the default time zone, so the day never shifts.
internal actual fun formatDate(date: LocalDate, languageTag: String): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag(languageTag)).format(date.toJavaDate())

internal actual fun formatMonth(date: LocalDate, languageTag: String): String {
    val locale = Locale.forLanguageTag(languageTag)
    return SimpleDateFormat(AndroidDateFormat.getBestDateTimePattern(locale, "LLLLyyyy"), locale).format(date.toJavaDate())
}

// 2 November 2026 is a Monday: "EEEEE" is the narrow name.
internal actual fun weekdayInitials(languageTag: String): List<String> {
    val format = SimpleDateFormat("EEEEE", Locale.forLanguageTag(languageTag))
    return (2..8).map { format.format(GregorianCalendar(2026, Calendar.NOVEMBER, it).time) }
}

// Calendar counts from Sunday = 1.
internal actual fun firstDayOfWeek(languageTag: String): DayOfWeek =
    DayOfWeek((Calendar.getInstance(Locale.forLanguageTag(languageTag)).firstDayOfWeek + 5) % 7 + 1)

private fun LocalDate.toJavaDate() = GregorianCalendar(year, month.ordinal, day).time
