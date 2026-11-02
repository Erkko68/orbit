package com.orbit.app.core

import kotlinx.datetime.LocalDate

/** A wall-clock date in the effective UI locale's regional format, without a time-zone conversion. */
internal expect fun formatDate(date: LocalDate, languageTag: String): String
