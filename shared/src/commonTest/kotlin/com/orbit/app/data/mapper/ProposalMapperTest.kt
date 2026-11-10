package com.orbit.app.data.mapper

import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ChecklistEntry
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.Recurrence
import com.orbit.app.domain.model.Reminder
import com.orbit.app.domain.model.Schedule
import com.orbit.app.domain.model.SpaceProposal
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes

class ProposalMapperTest {
    private val members = listOf("uid-ana", "uid-biel")

    private fun plan(items: String, space: String? = null): Plan {
        val spaceField = space?.let { ""","space":$it""" }.orEmpty()
        return planOf(Json.parseToJsonElement("""{"version":1$spaceField,"items":[$items]}""").jsonObject, members)
    }

    private fun item(modules: String, title: String = "Take out the bins") = """{"title":"$title","modules":{$modules}}"""

    private val bins = NewItem("Take out the bins")

    @Test
    fun readsEveryModuleAndMapsTheHandlesToTheMembers() {
        val proposal = """{"title":" Weekly shop ","description":"For the flat","modules":{""" +
            """"schedule":{"date":"2026-11-09","time":"18:30","durationMinutes":45},""" +
            """"recurrence":{"unit":"week","every":1,"weekdays":[1,4],"until":"2027-06-30"},""" +
            """"assignment":{"mode":"member","assignees":["m2","m1"]},""" +
            """"checklist":{"entries":[{"text":"Milk","quantity":2},{"text":"Bread"}]},""" +
            """"reminder":{"offsetsMinutes":[0,60]}}}"""

        val item = plan(proposal).items.single()

        assertEquals(
            NewItem(
                title = "Weekly shop",
                description = "For the flat",
                modules = ItemModules(
                    schedule = Schedule(LocalDate(2026, 11, 9), LocalTime(18, 30), duration = 45.minutes),
                    recurrence = Recurrence.Weeks(1, setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), LocalDate(2027, 6, 30)),
                    assignment = Assignment.ToMembers(setOf("uid-biel", "uid-ana")),
                    checklist = Checklist(
                        listOf(
                            ChecklistEntry("entry-0", "Milk", quantity = 2, order = 0),
                            ChecklistEntry("entry-1", "Bread", order = 1),
                        ),
                    ),
                    reminder = Reminder(listOf(0.minutes, 60.minutes)),
                ),
            ),
            item,
        )
    }

    @Test
    fun readsTheNewSpace() {
        assertEquals(SpaceProposal("Flat", 2), plan(item(""), space = """{"name":" Flat ","accent":2}""").space)
    }

    @Test
    fun dropsAnUnknownModule() {
        assertEquals(listOf(bins), plan("""${item(""""cost":{"amount":1200}""")},${item("")}""").items)
    }

    @Test
    fun dropsAMemberNotInTheSpace() {
        assertEquals(emptyList(), plan(item(""""assignment":{"mode":"member","assignees":["m1","m3"]}""")).items)
        assertEquals(emptyList(), plan(item(""""assignment":{"mode":"member","assignees":["Ana"]}""")).items)
    }

    @Test
    fun dropsAnEndBeforeItsStart() {
        assertEquals(emptyList(), plan(item(""""schedule":{"date":"2026-11-09","endDate":"2026-11-08"}""")).items)
        assertEquals(
            emptyList(),
            plan(item(""""schedule":{"date":"2026-11-09"},"recurrence":{"unit":"day","every":1,"until":"2026-11-01"}""")).items,
        )
    }

    @Test
    fun dropsAnEmptyTitle() {
        assertEquals(emptyList(), plan("""${item("", title = "  ")},${item("", title = "")}""").items)
    }

    @Test
    fun dropsWhatBreaksTheOtherRulesOfManualInput() {
        listOf(
            """"schedule":{"date":"2026-11-09","time":"21:00","endDate":"2026-11-10"}""",
            """"schedule":{"date":"2026-02-30"}""",
            """"schedule":{"date":"2026-11-09","durationMinutes":0}""",
            """"recurrence":{"unit":"day","every":1}""",
            """"schedule":{"date":"2026-11-09"},"recurrence":{"unit":"year","every":1}""",
            """"schedule":{"date":"2026-11-09"},"recurrence":{"unit":"week","every":1,"weekdays":[8]}""",
            """"schedule":{"date":"2026-11-09"},"recurrence":{"unit":"month","every":1}""",
            """"reminder":{"offsetsMinutes":[60]}""",
            """"assignment":{"mode":"member","assignees":[]}""",
            """"assignment":{"mode":"anyone","assignees":["m1"]}""",
            """"checklist":{"entries":[]}""",
            """"checklist":{"entries":[{"text":" "}]}""",
        ).forEach { modules -> assertEquals(emptyList(), plan(item(modules)).items, modules) }
    }

    @Test
    fun dropsWhatIsNotAnItem() {
        assertEquals(listOf(bins), plan("""{"modules":{}},"bins",{"title":"Bins"},${item("")}""").items)
    }

    @Test
    fun aPlanInAnotherVersionOrWithASpaceThatIsNotOneIsNotUsed() {
        listOf(
            """{"version":2,"items":[]}""",
            """{"items":[]}""",
            """{"version":1}""",
            """{"version":1,"items":[],"space":{"name":"Flat"}}""",
            """{"version":1,"items":[],"space":{"name":" ","accent":0}}""",
            """{"version":1,"items":[],"space":{"name":"Flat","accent":6}}""",
        ).forEach { body ->
            val failure = assertFailsWith<AssistantException>(body) {
                planOf(Json.parseToJsonElement(body).jsonObject, members)
            }
            assertEquals(AssistantError.UNAVAILABLE, failure.error, body)
        }
    }
}
