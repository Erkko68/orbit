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
    fun theClusterKeepsTurningAroundTheCentre() {
        val faces = listOf(FaceBody("a", 120f, 100f, 20f), FaceBody("b", 180f, 100f, 20f))
        settle(faces)
        val before = faces.map { it.x to it.y }
        val spread = hypot(faces[0].x - faces[1].x, faces[0].y - faces[1].y)

        settle(faces, frames = 120)

        assertTrue(hypot(faces[0].x - before[0].first, faces[0].y - before[0].second) > 5f, "the cluster stopped turning")
        assertEquals(spread, hypot(faces[0].x - faces[1].x, faces[0].y - faces[1].y), 2f)
        assertTrue(hypot(faces[0].x - WIDTH / 2, faces[0].y - HEIGHT / 2) < 25f)
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
