package com.orbit.app.data.repository

import androidx.compose.ui.text.intl.Locale
import com.orbit.app.core.membersOrYou
import com.orbit.app.data.mapper.InterviewContext
import com.orbit.app.data.mapper.interviewRequest
import com.orbit.app.data.mapper.interviewStep
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.repository.AssistantRepository
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Where the assistant server answers (ADR 0007): HTTPS through the Cloudflare tunnel to Èric's server. */
internal const val ASSISTANT_SERVER_URL = "https://orbit.biri.es"

/**
 * Calls `POST /interview` on the assistant server, as the signed-in user, with what the interview is about (#66): the
 * phone's language and day, and the space's member names and item titles. A new space has only the user.
 */
class ServerAssistantRepository(
    private val client: HttpClient,
    private val auth: AuthRepository,
    private val spaces: SpaceRepository,
    private val items: ItemRepository,
    private val baseUrl: String = ASSISTANT_SERVER_URL,
) : AssistantRepository {
    override suspend fun nextStep(spaceId: String, answers: List<InterviewAnswer>): InterviewStep {
        val token = auth.idToken()
        val members = orNetworkError { spaces.membersOrYou(spaceId, auth).first() }
        val titles = if (spaceId.isEmpty()) emptyList() else orNetworkError { items.observeItems(spaceId).first().map { it.title } }
        val context = InterviewContext(
            language = Locale.current.language,
            today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            newSpace = spaceId.isEmpty(),
            memberNames = members.map { it.displayName },
            itemTitles = titles,
        )
        val body = orNetworkError {
            client.post("${baseUrl.trimEnd('/')}/interview") {
                bearerAuth(token)
                setBody(TextContent(interviewRequest(answers, context), ContentType.Application.Json))
            }.bodyAsText()
        }
        // The server named the members m1, m2… in this order.
        return interviewStep(body, members = members.map { it.userId })
    }

    /** What the call needs from Firestore or the network: a failure means the assistant cannot be reached. */
    private suspend fun <T> orNetworkError(read: suspend () -> T): T = try {
        read()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw AssistantException(AssistantError.NETWORK, e)
    }
}
