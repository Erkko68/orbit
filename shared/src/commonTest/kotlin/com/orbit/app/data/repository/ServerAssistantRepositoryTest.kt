package com.orbit.app.data.repository

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.AssistantException
import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.testing.FailingAuthRepository
import com.orbit.app.testing.InMemoryAuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.io.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class ServerAssistantRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun answering(status: HttpStatusCode, body: String) = HttpClient(
        MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        },
    )

    private suspend fun signedIn() =
        InMemoryAuthRepository().apply { register("ana@orbit.app", "secret", "Ana López") }

    @Test
    fun asksTheServerAsTheSignedInUserWithEveryAnswer() = runTest {
        val client = answering(HttpStatusCode.OK, """{"result":{"id":"q2","text":"Which rooms?"}}""")
        val repository = ServerAssistantRepository(client, signedIn(), "https://assistant.test/")

        val step = repository.nextStep(listOf(InterviewAnswer("q1", setOf("Family"), "")))

        assertIs<InterviewStep.Ask>(step)
        val request = requests.single()
        assertEquals("https://assistant.test/interview", request.url.toString())
        assertEquals("Bearer token-ana@orbit.app", request.headers[HttpHeaders.Authorization])
        assertEquals(
            """{"data":{"answers":[{"questionId":"q1","selected":["Family"],"text":""}]}}""",
            (request.body as TextContent).text,
        )
    }

    @Test
    fun aRefusalComesBackAsItsError() = runTest {
        val client = answering(HttpStatusCode.ServiceUnavailable, """{"error":{"status":"UNAVAILABLE"}}""")

        val failure = assertFailsWith<AssistantException> {
            ServerAssistantRepository(client, signedIn()).nextStep(emptyList())
        }

        assertEquals(AssistantError.UNAVAILABLE, failure.error)
    }

    @Test
    fun noConnectionIsANetworkError() = runTest {
        val client = HttpClient(MockEngine { throw IOException("unreachable") })

        val failure = assertFailsWith<AssistantException> {
            ServerAssistantRepository(client, signedIn()).nextStep(emptyList())
        }

        assertEquals(AssistantError.NETWORK, failure.error)
    }

    @Test
    fun signedOutItAsksNothing() = runTest {
        val client = answering(HttpStatusCode.OK, "{}")

        assertFailsWith<IllegalStateException> {
            ServerAssistantRepository(client, FailingAuthRepository()).nextStep(emptyList())
        }
        assertEquals(0, requests.size)
    }
}
