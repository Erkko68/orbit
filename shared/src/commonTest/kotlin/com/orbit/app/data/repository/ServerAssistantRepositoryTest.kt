package com.orbit.app.data.repository

import com.orbit.app.domain.model.AssistantError
import com.orbit.app.domain.model.Assignment
import com.orbit.app.domain.model.Member
import com.orbit.app.domain.model.NewItem
import com.orbit.app.domain.model.Checklist
import com.orbit.app.domain.model.ItemModules
import com.orbit.app.domain.model.Removal
import com.orbit.app.domain.model.Role
import com.orbit.app.domain.model.Space
import com.orbit.app.domain.repository.AuthRepository
import com.orbit.app.domain.repository.ItemRepository
import com.orbit.app.domain.repository.SpaceRepository
import com.orbit.app.testing.FakeSpaceRepository
import com.orbit.app.testing.InMemoryItemRepository
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    private val spaces = FakeSpaceRepository.withMembers("ana@orbit.app" to Role.OWNER, "ben@orbit.app" to Role.MEMBER)

    private suspend fun signedIn() =
        InMemoryAuthRepository().apply { register("ana@orbit.app", "secret", "Ana López") }

    private fun repository(client: HttpClient, auth: AuthRepository, items: ItemRepository = InMemoryItemRepository(auth)) =
        ServerAssistantRepository(client, auth, spaces, items, "https://assistant.test/")

    private fun sent(): JsonObject = Json.parseToJsonElement((requests.single().body as TextContent).text).jsonObject

    @Test
    fun asksTheServerAsTheSignedInUserWithEveryAnswerAndTheSpace() = runTest {
        val client = answering(HttpStatusCode.OK, """{"result":{"id":"q2","text":"Which rooms?"}}""")
        val auth = signedIn()
        val items = InMemoryItemRepository(auth).apply { create("space-1", NewItem("Bins")) }

        val step = repository(client, auth, items).nextStep("space-1", listOf(InterviewAnswer("q1", "Who?", setOf("Family"), "")))

        assertIs<InterviewStep.Ask>(step)
        val request = requests.single()
        assertEquals("https://assistant.test/interview", request.url.toString())
        assertEquals("Bearer token-ana@orbit.app", request.headers[HttpHeaders.Authorization])
        val data = sent().getValue("data").jsonObject
        assertEquals("""[{"questionId":"q1","question":"Who?","selected":["Family"],"text":""}]""", data["answers"].toString())
        // Names only: no uid or email reaches the server.
        assertEquals("""[{"name":"ana@orbit.app"},{"name":"ben@orbit.app"}]""", data["members"].toString())
        assertEquals("""["Bins"]""", data["items"].toString())
        assertEquals("false", data["newSpace"].toString())
        assertEquals("Flat 4B", data["space"]?.jsonPrimitive?.content)
        assertEquals(2, data["language"]?.jsonPrimitive?.content?.length)
    }

    @Test
    fun theUserIsTheFirstMemberSoAPlanForM1IsTheirs() = runTest {
        val plan = """{"version":1,"items":[{"title":"Bins","modules":{"assignment":{"mode":"member","assignees":["m1"]}}}]}"""
        val client = answering(HttpStatusCode.OK, """{"result":{"plan":$plan}}""")
        val ben = InMemoryAuthRepository().apply { register("ben@orbit.app", "secret", "Ben") }

        val step = repository(client, ben).nextStep("space-1", emptyList())

        assertEquals("""[{"name":"ben@orbit.app"},{"name":"ana@orbit.app"}]""", sent().getValue("data").jsonObject["members"].toString())
        val item = assertIs<InterviewStep.PlanReady>(step).plan.items.single()
        assertEquals(Assignment.ToMembers(setOf("ben@orbit.app")), item.modules.assignment)
    }

    @Test
    fun aRemovalOffersOnlyWhatTheUserMayDeleteAndComesBackAsTheirItems() = runTest {
        val removal = """{"items":[{"item":"i2","reason":"Not done since March"},{"item":"i9","reason":"Not sent"},{"item":"i2"}]}"""
        val client = answering(HttpStatusCode.OK, """{"result":{"removal":$removal}}""")
        val auth = signedIn()
        val items = InMemoryItemRepository(auth)
        items.create("space-1", NewItem("Bins"))
        auth.register("ben@orbit.app", "secret", "Ben")
        items.create("space-1", NewItem("Plants"))
        val list = items.create("space-1", NewItem("Party shopping", modules = ItemModules(checklist = Checklist(emptyList()))))

        // Ben is a member: he may delete only what he created.
        val step = repository(client, auth, items).nextStep("space-1", emptyList())

        val data = sent().getValue("data").jsonObject
        assertEquals("""["Bins","Plants","Party shopping"]""", data["items"].toString())
        assertEquals(
            """[{"title":"Plants","kind":"task"},{"title":"Party shopping","kind":"list"}]""",
            data["removable"].toString(),
        )
        assertEquals(listOf(Removal(list, "Party shopping", "Not done since March")), assertIs<InterviewStep.RemovalReady>(step).removals)
    }

    @Test
    fun anOwnerMayRemoveAnyItem() = runTest {
        val client = answering(HttpStatusCode.OK, """{"result":{"removal":{"items":[]}}}""")
        val auth = signedIn()
        val items = InMemoryItemRepository(auth)
        auth.register("ben@orbit.app", "secret", "Ben")
        items.create("space-1", NewItem("Plants"))
        auth.register("ana@orbit.app", "secret", "Ana López")

        val step = repository(client, auth, items).nextStep("space-1", emptyList())

        assertEquals("""[{"title":"Plants","kind":"task"}]""", sent().getValue("data").jsonObject["removable"].toString())
        assertEquals(emptyList(), assertIs<InterviewStep.RemovalReady>(step).removals)
    }

    @Test
    fun aNewSpaceHasOnlyTheUserAndAPlanAssignsByTheirHandle() = runTest {
        val plan = """{"version":1,"items":[{"title":"Bins","modules":{"assignment":{"mode":"member","assignees":["m1"]}}}]}"""
        val client = answering(HttpStatusCode.OK, """{"result":{"plan":$plan}}""")

        val step = repository(client, signedIn()).nextStep("", emptyList())

        val data = sent().getValue("data").jsonObject
        assertEquals("""[{"name":"Ana López"}]""", data["members"].toString())
        assertEquals("[]", data["items"].toString())
        assertEquals("true", data["newSpace"].toString())
        val item = assertIs<InterviewStep.PlanReady>(step).plan.items.single()
        assertEquals(Assignment.ToMembers(setOf("ana@orbit.app")), item.modules.assignment)
    }

    @Test
    fun aSpaceThatCannotBeReadIsANetworkError() = runTest {
        val failing = object : SpaceRepository by spaces {
            override fun observeMembers(spaceId: String) = flow<List<Member>> { error("offline") }
        }
        val auth = signedIn()
        val repository = ServerAssistantRepository(answering(HttpStatusCode.OK, "{}"), auth, failing, InMemoryItemRepository(auth))

        val failure = assertFailsWith<AssistantException> { repository.nextStep("space-1", emptyList()) }

        assertEquals(AssistantError.NETWORK, failure.error)
        assertEquals(0, requests.size)
    }

    @Test
    fun aRefusalComesBackAsItsError() = runTest {
        val client = answering(HttpStatusCode.ServiceUnavailable, """{"error":{"status":"UNAVAILABLE"}}""")

        val failure = assertFailsWith<AssistantException> {
            repository(client, signedIn()).nextStep("space-1", emptyList())
        }

        assertEquals(AssistantError.UNAVAILABLE, failure.error)
    }

    @Test
    fun noConnectionIsANetworkError() = runTest {
        val client = HttpClient(MockEngine { throw IOException("unreachable") })

        val failure = assertFailsWith<AssistantException> {
            repository(client, signedIn()).nextStep("space-1", emptyList())
        }

        assertEquals(AssistantError.NETWORK, failure.error)
    }

    @Test
    fun signedOutItAsksNothing() = runTest {
        val client = answering(HttpStatusCode.OK, "{}")

        assertFailsWith<IllegalStateException> {
            repository(client, FailingAuthRepository()).nextStep("space-1", emptyList())
        }
        assertEquals(0, requests.size)
    }
}
