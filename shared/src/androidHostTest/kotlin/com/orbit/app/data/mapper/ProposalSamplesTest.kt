package com.orbit.app.data.mapper

import com.orbit.app.domain.model.InterviewStep
import java.io.File
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The server's samples in `server/schemas/samples/` parse here as they do in its pytest, so the app and the server
 * read one contract (#64). Android host only: it reads them from the repository.
 */
class ProposalSamplesTest {
    private val samples: List<File> = generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, "server/schemas/samples") }
        .first { it.isDirectory }
        .listFiles { file -> file.extension == "json" }
        .orEmpty()
        .sortedBy { it.name }

    @Test
    fun thereAreSamplesToRead() {
        assertTrue(samples.isNotEmpty())
    }

    /** Every proposal of every sample passes the checks of manual input, so none is dropped (#65). */
    @Test
    fun everySampleIsAPlanTheAppShowsWhole() {
        samples.forEach { sample ->
            val reply = """{"result":{"plan":${sample.readText()}}}"""
            val proposed = Json.decodeFromString(PlanDocument.serializer(), sample.readText()).items.size

            val step = interviewStep(reply, members = listOf("uid-1", "uid-2", "uid-3"))

            assertEquals(proposed, assertIs<InterviewStep.PlanReady>(step, sample.name).plan.items.size, sample.name)
        }
    }

    /** The strict default `Json`, unlike the reply's: a field the types do not know fails instead of being skipped. */
    @Test
    fun everyFieldOfEverySampleHasItsPlaceInTheTypes() {
        samples.forEach { sample ->
            val json = Json.parseToJsonElement(sample.readText())
            val plan = Json.decodeFromJsonElement(PlanDocument.serializer(), json)

            assertEquals(json, Json.encodeToJsonElement(PlanDocument.serializer(), plan), sample.name)
        }
    }
}
