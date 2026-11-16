package com.orbit.app.testing

import com.orbit.app.domain.model.InterviewAnswer
import com.orbit.app.domain.model.InterviewStep
import com.orbit.app.domain.repository.AssistantRepository

/**
 * An assistant that answers with [steps] in order, one per call that does not fail. The call numbered n (from 0)
 * fails with `failures[n]` when there is one. Records the answers each call got in [calls], and its space in [spaceIds].
 */
internal class ScriptedAssistantRepository(
    private val steps: List<InterviewStep>,
    private val failures: Map<Int, Exception> = emptyMap(),
) : AssistantRepository {
    private var next = 0

    val calls = mutableListOf<List<InterviewAnswer>>()
    val spaceIds = mutableListOf<String>()

    override suspend fun nextStep(spaceId: String, answers: List<InterviewAnswer>): InterviewStep {
        val failure = failures[calls.size]
        calls += answers
        spaceIds += spaceId
        if (failure != null) throw failure
        return steps[next++]
    }
}
