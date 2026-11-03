package com.orbit.app.data.repository

import com.orbit.app.data.mapper.interviewRequest
import com.orbit.app.data.mapper.interviewStep
import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.repository.AssistantRepository
import com.orbit.app.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException

/**
 * Where the assistant server answers (ADR 0007): HTTPS behind the reverse proxy on Èric's server. Pending its domain;
 * until then every call fails as no connection, and the manual path stays.
 */
internal const val ASSISTANT_SERVER_URL = "https://assistant.orbit.invalid"

/** Calls `POST /interview` on the assistant server, as the signed-in user. */
class ServerAssistantRepository(
    private val client: HttpClient,
    private val auth: AuthRepository,
    private val baseUrl: String = ASSISTANT_SERVER_URL,
) : AssistantRepository {
    override suspend fun nextStep(answers: List<InterviewAnswer>): InterviewStep {
        val token = auth.idToken()
        val body = try {
            client.post("${baseUrl.trimEnd('/')}/interview") {
                bearerAuth(token)
                setBody(TextContent(interviewRequest(answers), ContentType.Application.Json))
            }.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AssistantException(AssistantError.NETWORK, e)
        }
        return interviewStep(body)
    }
}
