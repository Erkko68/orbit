package com.orbit.app.data.mapper

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

/** The body of `POST /interview` for every answer so far. */
internal fun interviewRequest(answers: List<InterviewAnswer>): String = json.encodeToString(
    InterviewRequest.serializer(),
    InterviewRequest(InterviewRequestData(answers.map { AnswerDocument(it.questionId, it.selected.toList(), it.text) })),
)

/**
 * The step in the server's reply, or the [AssistantException] it stands for. A reply that is not the callable shape,
 * such as a proxy's error page, means the server is not answering. [members] are the uids the request names, in the
 * order of their handles (`m1` first).
 */
internal fun interviewStep(body: String, members: List<String>): InterviewStep {
    val reply = try {
        json.decodeFromString(InterviewReply.serializer(), body)
    } catch (e: SerializationException) {
        throw AssistantException(AssistantError.UNAVAILABLE, e)
    } catch (e: IllegalArgumentException) {
        throw AssistantException(AssistantError.UNAVAILABLE, e)
    }
    reply.error?.let { throw AssistantException(assistantError(it.status)) }
    val step = reply.result ?: throw AssistantException(AssistantError.UNAVAILABLE)
    step.plan?.let { return InterviewStep.PlanReady(planOf(it, members)) }
    val id = step.id
    val text = step.text
    if (id.isNullOrBlank() || text.isNullOrBlank()) throw AssistantException(AssistantError.UNAVAILABLE)
    return InterviewStep.Ask(
        InterviewQuestion(id, text, step.options, step.multiSelect, step.freeText, step.position, step.budget),
    )
}

/** The callable status the server sends for a refusal (#67, #68) or a failure. */
internal fun assistantError(status: String): AssistantError = when (status) {
    "RESOURCE_EXHAUSTED" -> AssistantError.QUOTA_EXCEEDED
    "FAILED_PRECONDITION" -> AssistantError.DISABLED
    "UNAVAILABLE" -> AssistantError.UNAVAILABLE
    else -> AssistantError.UNKNOWN
}
