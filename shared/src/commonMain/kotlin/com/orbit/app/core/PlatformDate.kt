package com.orbit.app.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** A wall-clock date in the effective UI locale's regional format, without a time-zone conversion. */
internal expect fun formatDate(date: LocalDate, languageTag: String): String

/** The month and year of [date] as the locale names them, such as "November 2026". */
internal expect fun formatMonth(date: LocalDate, languageTag: String): String

/** The shortest names of the days of the week, Monday first, such as "M", "T", "W". */
internal expect fun weekdayInitials(languageTag: String): List<String>

/** The day weeks start on in the locale's region: Monday in Spain, Sunday in the US. */
internal expect fun firstDayOfWeek(languageTag: String): DayOfWeek

/** [date] in the platform's regional format, in the app's locale. */
@Composable
internal fun dateLabel(date: LocalDate): String = formatDate(date, Locale.current.toLanguageTag())
