package com.orbit.app.data.mapper

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/* The callable shape the assistant server speaks (ADR 0007): {"data": …} in, {"result": …} or {"error": …} out. */

@Serializable
internal data class InterviewRequest(val data: InterviewRequestData)

@Serializable
internal data class InterviewRequestData(val answers: List<AnswerDocument>)

@Serializable
internal data class AnswerDocument(val questionId: String, val selected: List<String>, val text: String)

@Serializable
internal data class InterviewReply(val result: StepDocument? = null, val error: ErrorDocument? = null)

/** A question, or a plan once the server has one (#66). */
@Serializable
internal data class StepDocument(
    val id: String? = null,
    val text: String? = null,
    val options: List<String> = emptyList(),
    val multiSelect: Boolean = false,
    val freeText: Boolean = false,
    val position: Int = 0,
    val budget: Int = 0,
    val plan: JsonElement? = null,
)

@Serializable
internal data class ErrorDocument(val status: String, val message: String? = null)

private val json = Json { ignoreUnknownKeys = true }

/** The body of `POST /interview` for every answer so far. */
internal fun interviewRequest(answers: List<InterviewAnswer>): String = json.encodeToString(
    InterviewRequest.serializer(),
    InterviewRequest(InterviewRequestData(answers.map { AnswerDocument(it.questionId, it.selected.toList(), it.text) })),
)

/**
 * The step in the server's reply, or the [AssistantException] it stands for. A reply that is not the callable shape,
 * such as a proxy's error page, means the server is not answering.
 */
internal fun interviewStep(body: String): InterviewStep {
    val reply = try {
        json.decodeFromString(InterviewReply.serializer(), body)
    } catch (e: SerializationException) {
        throw AssistantException(AssistantError.UNAVAILABLE, e)
    } catch (e: IllegalArgumentException) {
        throw AssistantException(AssistantError.UNAVAILABLE, e)
    }
    reply.error?.let { throw AssistantException(assistantError(it.status)) }
    val step = reply.result ?: throw AssistantException(AssistantError.UNAVAILABLE)
    if (step.plan != null) return InterviewStep.PlanReady
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
