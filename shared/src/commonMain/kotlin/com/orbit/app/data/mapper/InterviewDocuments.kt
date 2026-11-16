package com.orbit.app.data.mapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/* The callable shape the assistant server speaks (ADR 0007): {"data": …} in, {"result": …} or {"error": …} out. */

@Serializable
internal data class InterviewRequest(val data: InterviewRequestData)

/**
 * Everything the server needs for the next step (#66): it keeps no state. Members go by name only, never uid or email:
 * the server gives each a handle by its place in [members] (`m1` first).
 */
@Serializable
internal data class InterviewRequestData(
    val answers: List<AnswerDocument>,
    val language: String,
    val today: String,
    val newSpace: Boolean,
    val members: List<InterviewMemberDocument>,
    val items: List<String>,
)

@Serializable
internal data class AnswerDocument(val questionId: String, val question: String, val selected: List<String>, val text: String)

@Serializable
internal data class InterviewMemberDocument(val name: String)

@Serializable
internal data class InterviewReply(val result: StepDocument? = null, val error: ErrorDocument? = null)

/** A question, or a plan once the server has one (#66), read proposal by proposal (`planOf`). */
@Serializable
internal data class StepDocument(
    val id: String? = null,
    val text: String? = null,
    val options: List<String> = emptyList(),
    val multiSelect: Boolean = false,
    val freeText: Boolean = false,
    val position: Int = 0,
    val budget: Int = 0,
    val plan: JsonObject? = null,
)

@Serializable
internal data class ErrorDocument(val status: String, val message: String? = null)
