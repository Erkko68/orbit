package com.orbit.app.data.mapper

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewQuestion
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.model.Item
import com.orbit.app.domain.model.ItemStatus
import com.orbit.app.domain.model.Removal
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Instant

private val json = Json { ignoreUnknownKeys = true }

/**
 * What the interview is about: the device's [language] and day, the space's name (empty for a new one), member
 * names, the user's first, item titles, and the items the user may delete, for a removal request (#89).
 */
internal data class InterviewContext(
    val language: String,
    val today: LocalDate,
    val newSpace: Boolean,
    val spaceName: String,
    val memberNames: List<String>,
    val itemTitles: List<String>,
    val removable: List<Item> = emptyList(),
)

/** The body of `POST /interview` for every answer so far. A member without a name is still listed, as one. */
internal fun interviewRequest(answers: List<InterviewAnswer>, context: InterviewContext): String = json.encodeToString(
    InterviewRequest.serializer(),
    InterviewRequest(
        InterviewRequestData(
            answers = answers.map { AnswerDocument(it.questionId, it.question, it.selected.toList(), it.text, it.decided) },
            language = context.language,
            today = context.today.toString(),
            newSpace = context.newSpace,
            members = context.memberNames.mapIndexed { index, name -> InterviewMemberDocument(name.ifBlank { "Member ${index + 1}" }) },
            items = context.itemTitles,
            space = context.spaceName,
            removable = context.removable.map(::removableDocument),
        ),
    ),
)

/** A list is an item with a checklist, an event one at a time of day; anything else is a task. */
private fun removableDocument(item: Item) = RemovableDocument(
    title = item.title,
    kind = when {
        item.modules.checklist != null -> "list"
        item.modules.schedule?.time != null -> "event"
        else -> "task"
    },
    lastDone = (item.status as? ItemStatus.Done)?.at?.toLocalDateTime(TimeZone.currentSystemDefault())?.date?.toString(),
)

/**
 * The step in the server's reply, or the [AssistantException] it stands for. A reply that is not the callable shape,
 * such as a proxy's error page, means the server is not answering. [members] are the uids the request names, in the
 * order of their handles (`m1` first), and [removable] the items it offered to remove, in the order of theirs (`i1`).
 */
internal fun interviewStep(body: String, members: List<String>, removable: List<Item> = emptyList()): InterviewStep {
    val reply = try {
        json.decodeFromString(InterviewReply.serializer(), body)
    } catch (e: SerializationException) {
        throw AssistantException(AssistantError.UNAVAILABLE, e)
    } catch (e: IllegalArgumentException) {
        throw AssistantException(AssistantError.UNAVAILABLE, e)
    }
    reply.error?.let { error ->
        val resetsAt = error.details?.resetsAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
        throw AssistantException(assistantError(error.status), resetsAt = resetsAt)
    }
    val step = reply.result ?: throw AssistantException(AssistantError.UNAVAILABLE)
    step.plan?.let { return InterviewStep.PlanReady(planOf(it, members)) }
    step.removal?.let { return InterviewStep.RemovalReady(removalsOf(it, removable)) }
    val id = step.id
    val text = step.text
    if (id.isNullOrBlank() || text.isNullOrBlank()) throw AssistantException(AssistantError.UNAVAILABLE)
    return InterviewStep.Ask(
        InterviewQuestion(
            id, text, step.options, step.multiSelect, step.freeText,
            step.suggestion?.takeIf(String::isNotBlank),
        ),
    )
}

/** Each handle back to its item. One the request did not send, or a repeat, is dropped: the server checks it too. */
private fun removalsOf(removal: RemovalDocument, removable: List<Item>): List<Removal> =
    removal.items.mapNotNull { proposed ->
        val index = proposed.item.removePrefix("i").toIntOrNull()?.minus(1) ?: return@mapNotNull null
        removable.getOrNull(index)?.let { Removal(it.id, it.title, proposed.reason.trim()) }
    }.distinctBy { it.itemId }

/** The callable status the server sends for a refusal (#67, #68) or a failure. */
internal fun assistantError(status: String): AssistantError = when (status) {
    "RESOURCE_EXHAUSTED" -> AssistantError.QUOTA_EXCEEDED
    "FAILED_PRECONDITION" -> AssistantError.DISABLED
    "UNAVAILABLE" -> AssistantError.UNAVAILABLE
    else -> AssistantError.UNKNOWN
}
