package com.orbit.app.data.mapper

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.model.Plan
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Removal
import com.orbit.app.domain.model.Schedule
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlin.test.assertTrue

class InterviewMapperTest {
    private fun failure(body: String) =
        assertFailsWith<AssistantException> { interviewStep(body, members = emptyList()) }.error

    @Test
    fun sendsEveryAnswerInTheCallableShape() {
        val context = InterviewContext(
            "es", LocalDate(2026, 11, 16), newSpace = false, "Flat 4B", listOf("Ana", " "), listOf("Bins"),
        )
        val body = interviewRequest(listOf(InterviewAnswer("q1", "Who lives there?", setOf("Family"), "Three of us")), context)

        assertEquals(
            """{"data":{"answers":[{"questionId":"q1","question":"Who lives there?","selected":["Family"],""" +
                """"text":"Three of us"}],"language":"es","today":"2026-11-16","newSpace":false,""" +
                """"members":[{"name":"Ana"},{"name":"Member 2"}],"items":["Bins"],"space":"Flat 4B"}}""",
            body,
        )
    }

    @Test
    fun sendsTheRemovableItemsWithTheirKindAndTheDayEachWasLastDone() {
        val doneAt = LocalDateTime(2026, 3, 2, 12, 0).toInstant(TimeZone.currentSystemDefault())
        fun item(id: String, modules: ItemModules = ItemModules(), status: ItemStatus = ItemStatus.Open) =
            Item(id, "Title $id", null, status, "ana", doneAt, modules)
        val context = InterviewContext(
            "en", LocalDate(2026, 11, 16), newSpace = false, "Flat 4B", listOf("Ana"), emptyList(),
            removable = listOf(
                item("a", status = ItemStatus.Done("ana", doneAt)),
                item("b", ItemModules(schedule = Schedule(LocalDate(2026, 12, 1), time = LocalTime(18, 0)))),
            ),
        )

        val body = interviewRequest(emptyList(), context)

        assertTrue(
            """"removable":[{"title":"Title a","kind":"task","lastDone":"2026-03-02"},{"title":"Title b","kind":"event"}]""" in body,
        )
    }

    @Test
    fun aRemovalWithAHandleThatWasNotSentKeepsOnlyTheOthers() {
        val bins = Item("bins", "Bins", null, ItemStatus.Open, "ana", Instant.fromEpochSeconds(0))
        val step = interviewStep(
            """{"result":{"removal":{"items":[{"item":"x1"},{"item":"i1","reason":" Old "},{"item":"i5"}]}}}""",
            members = emptyList(),
            removable = listOf(bins),
        )

        assertEquals(InterviewStep.RemovalReady(listOf(Removal("bins", "Bins", "Old"))), step)
    }

    @Test
    fun readsAQuestion() {
        val step = interviewStep(
            """{"result":{"id":"q1","text":"Who lives there?","options":["Roommates"],"multiSelect":false,""" +
                """"freeText":true,"position":1,"extra":"ignored"}}""",
            members = emptyList(),
        )

        assertEquals(
            InterviewStep.Ask(InterviewQuestion("q1", "Who lives there?", listOf("Roommates"), false, true)),
            step,
        )
    }

    @Test
    fun readsAPlan() {
        assertEquals(
            InterviewStep.PlanReady(Plan(space = null, items = emptyList())),
            interviewStep("""{"result":{"plan":{"version":1,"items":[]}}}""", members = emptyList()),
        )
    }

    @Test
    fun aPlanThatIsNotAnObjectIsNotUsed() {
        assertEquals(AssistantError.UNAVAILABLE, failure("""{"result":{"plan":"Take out the bins"}}"""))
    }

    @Test
    fun readsTheSuggestionAndSendsWhetherTheAssistantDecided() {
        val body = """{"result":{"id":"q1","text":"Who?","options":["A","B"],"position":1,"suggestion":"B"}}"""
        val step = interviewStep(body, members = emptyList()) as InterviewStep.Ask

        assertEquals("B", step.question.suggestion)
        val context = InterviewContext("en", LocalDate(2026, 11, 17), false, "", emptyList(), emptyList())
        val request = interviewRequest(listOf(InterviewAnswer("q1", "Who?", setOf("B"), "", decided = true)), context)
        assertTrue(""""decided":true""" in request)
    }

    @Test
    fun readsTheServersRefusalsAndFailures() {
        assertEquals(AssistantError.QUOTA_EXCEEDED, failure("""{"error":{"status":"RESOURCE_EXHAUSTED"}}"""))
        assertEquals(AssistantError.DISABLED, failure("""{"error":{"status":"FAILED_PRECONDITION"}}"""))
        assertEquals(AssistantError.UNAVAILABLE, failure("""{"error":{"status":"UNAVAILABLE","message":"down"}}"""))
        assertEquals(AssistantError.UNKNOWN, failure("""{"error":{"status":"UNAUTHENTICATED"}}"""))
    }

    @Test
    fun aSpentQuotaSaysWhenItResets() {
        val body = """{"error":{"status":"RESOURCE_EXHAUSTED","details":{"reason":"quota","resetsAt":"2026-11-18T00:00:00Z"}}}"""

        val refusal = assertFailsWith<AssistantException> { interviewStep(body, members = emptyList()) }

        assertEquals(AssistantError.QUOTA_EXCEEDED, refusal.error)
        assertEquals(Instant.parse("2026-11-18T00:00:00Z"), refusal.resetsAt)
    }

    @Test
    fun aResetThatIsNotAnInstantIsLeftOut() {
        val body = """{"error":{"status":"RESOURCE_EXHAUSTED","details":{"resetsAt":"tomorrow"}}}"""

        assertNull(assertFailsWith<AssistantException> { interviewStep(body, members = emptyList()) }.resetsAt)
    }

    @Test
    fun aReplyThatIsNotTheCallableShapeMeansTheServerIsNotAnswering() {
        assertEquals(AssistantError.UNAVAILABLE, failure("<html>502 Bad Gateway</html>"))
        assertEquals(AssistantError.UNAVAILABLE, failure("{}"))
        assertEquals(AssistantError.UNAVAILABLE, failure("""{"result":{"text":"No id"}}"""))
    }
}
