package com.orbit.app.data.mapper

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.model.Plan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InterviewMapperTest {
    private fun failure(body: String) =
        assertFailsWith<AssistantException> { interviewStep(body, members = emptyList()) }.error

    @Test
    fun sendsEveryAnswerInTheCallableShape() {
        val body = interviewRequest(listOf(InterviewAnswer("q1", setOf("Family"), "Three of us")))

        assertEquals("""{"data":{"answers":[{"questionId":"q1","selected":["Family"],"text":"Three of us"}]}}""", body)
    }

    @Test
    fun readsAQuestion() {
        val step = interviewStep(
            """{"result":{"id":"q1","text":"Who lives there?","options":["Roommates"],"multiSelect":false,""" +
                """"freeText":true,"position":1,"budget":8,"extra":"ignored"}}""",
            members = emptyList(),
        )

        assertEquals(
            InterviewStep.Ask(InterviewQuestion("q1", "Who lives there?", listOf("Roommates"), false, true, 1, 8)),
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
    fun readsTheServersRefusalsAndFailures() {
        assertEquals(AssistantError.QUOTA_EXCEEDED, failure("""{"error":{"status":"RESOURCE_EXHAUSTED"}}"""))
        assertEquals(AssistantError.DISABLED, failure("""{"error":{"status":"FAILED_PRECONDITION"}}"""))
        assertEquals(AssistantError.UNAVAILABLE, failure("""{"error":{"status":"UNAVAILABLE","message":"down"}}"""))
        assertEquals(AssistantError.UNKNOWN, failure("""{"error":{"status":"UNAUTHENTICATED"}}"""))
    }

    @Test
    fun aReplyThatIsNotTheCallableShapeMeansTheServerIsNotAnswering() {
        assertEquals(AssistantError.UNAVAILABLE, failure("<html>502 Bad Gateway</html>"))
        assertEquals(AssistantError.UNAVAILABLE, failure("{}"))
        assertEquals(AssistantError.UNAVAILABLE, failure("""{"result":{"text":"No id"}}"""))
    }
}
