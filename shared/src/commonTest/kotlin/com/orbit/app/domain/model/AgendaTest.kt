package com.orbit.app.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class AgendaTest {

    private val monday = LocalDate(2026, 11, 2)
    private val tuesday = LocalDate(2026, 11, 3)
    private val wednesday = LocalDate(2026, 11, 4)
    private val thursday = LocalDate(2026, 11, 5)
    private val friday = LocalDate(2026, 11, 6)
    private val sunday = LocalDate(2026, 11, 8)
    private val nextMonday = LocalDate(2026, 11, 9)

    private fun item(id: String, schedule: Schedule?, status: ItemStatus = ItemStatus.Open) = Item(
        id = id,
        title = id,
        description = null,
        status = status,
        createdBy = "ana",
        createdAt = Instant.fromEpochSeconds(0),
        modules = ItemModules(schedule = schedule),
    )

    private fun Agenda.ids() = listOf(overdue, today, thisWeek).map { section -> section.map { it.id } }

    @Test
    fun sortsOpenItemsIntoOverdueTodayAndThisWeekInTheOrderTheyStart() {
        val items = listOf(
            item("evening", Schedule(wednesday, time = LocalTime(18, 0))),
            item("sunday", Schedule(sunday)),
            item("all day", Schedule(wednesday)),
            item("monday", Schedule(monday)),
            item("window ended", Schedule(monday, endDate = tuesday)),
            item("window open", Schedule(monday, endDate = friday)),
            item("thursday", Schedule(thursday)),
            item("next week", Schedule(nextMonday)),
            item("undated", null),
            item("done", Schedule(wednesday), ItemStatus.Done("ana", Instant.fromEpochSeconds(0))),
        )

        assertEquals(
            listOf(
                listOf("monday", "window ended"),
                listOf("window open", "all day", "evening"),
                listOf("thursday", "sunday"),
            ),
            agendaOf(items, wednesday).ids(),
        )
    }

    @Test
    fun theWeekEndsOnSunday() {
        assertEquals(sunday, endOfWeek(monday))
        assertEquals(sunday, endOfWeek(wednesday))
        assertEquals(sunday, endOfWeek(sunday))
        assertEquals(LocalDate(2027, 1, 3), endOfWeek(LocalDate(2026, 12, 31)))
    }

    @Test
    fun onSundayNextWeekIsNotThisWeek() {
        val items = listOf(item("sunday", Schedule(sunday)), item("monday", Schedule(nextMonday)))

        assertEquals(listOf(emptyList(), listOf("sunday"), emptyList()), agendaOf(items, sunday).ids())
    }

    @Test
    fun theSameMomentIsADifferentDayInAnotherTimeZone() {
        val madrid = TimeZone.of("Europe/Madrid")
        val losAngeles = TimeZone.of("America/Los_Angeles")
        // Thursday 00:30 in Madrid is still Wednesday afternoon in Los Angeles.
        val now = LocalDateTime(thursday, LocalTime(0, 30)).toInstant(madrid)
        val items = listOf(item("thursday", Schedule(thursday)))

        assertEquals(listOf(emptyList(), listOf("thursday"), emptyList()), agendaOf(items, now.dayIn(madrid)).ids())
        assertEquals(listOf(emptyList(), emptyList(), listOf("thursday")), agendaOf(items, now.dayIn(losAngeles)).ids())
    }

    @Test
    fun theWeekTurnsOverAtEachPhonesMidnight() {
        // Monday 00:30 in Madrid: a new week there, still last Sunday in Los Angeles.
        val now = LocalDateTime(nextMonday, LocalTime(0, 30)).toInstant(TimeZone.of("Europe/Madrid"))

        assertEquals(LocalDate(2026, 11, 15), endOfWeek(now.dayIn(TimeZone.of("Europe/Madrid"))))
        assertEquals(sunday, endOfWeek(now.dayIn(TimeZone.of("America/Los_Angeles"))))
    }

    private fun Instant.dayIn(zone: TimeZone) = toLocalDateTime(zone).date
}
