package com.orbit.app.core.designsystem.component

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FacePhysicsTest {
    private val step = 1 / 60f

    private fun settle(bodies: List<FaceBody>, frames: Int = 600) {
        repeat(frames) { stepFaces(bodies, WIDTH, HEIGHT, WIDTH / 2, HEIGHT / 2, step) }
    }

    @Test
    fun facesGrowToTheirSizeAndGatherAroundTheCentre() {
        val body = FaceBody("a", 10f, 10f, targetRadius = 20f)
        settle(listOf(body))
        assertEquals(20f, body.radius, 0.5f)
        assertTrue(hypot(body.x - WIDTH / 2, body.y - HEIGHT / 2) < 10f)
    }

    @Test
    fun facesOnTopOfEachOtherPushApart() {
        val first = FaceBody("a", 100f, 100f, 20f)
        val second = FaceBody("b", 100f, 100f, 20f)
        settle(listOf(first, second))
        assertTrue(hypot(first.x - second.x, first.y - second.y) >= 40f - 0.5f)
    }

    @Test
    fun aThrownFaceStaysOnTheStage() {
        val body = FaceBody("a", 100f, 100f, 20f)
        body.release(99_999f, -99_999f)
        repeat(240) {
            stepFaces(listOf(body), WIDTH, HEIGHT, WIDTH / 2, HEIGHT / 2, step)
            assertTrue(body.x in body.radius..WIDTH - body.radius)
            assertTrue(body.y in body.radius..HEIGHT - body.radius)
        }
    }

    @Test
    fun aHeldFaceStaysUnderTheFingerAndPushesTheOthers() {
        val held = FaceBody("a", 100f, 100f, 20f).apply { held = true }
        val other = FaceBody("b", 100f, 100f, 20f)
        settle(listOf(held, other))
        assertEquals(100f, held.x)
        assertEquals(100f, held.y)
        assertTrue(hypot(held.x - other.x, held.y - other.y) >= 40f - 0.5f)
    }

    private companion object {
        const val WIDTH = 300f
        const val HEIGHT = 200f
    }
}
