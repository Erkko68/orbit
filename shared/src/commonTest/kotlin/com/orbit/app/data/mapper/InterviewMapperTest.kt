package com.orbit.app.data.mapper

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.model.Plan
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
