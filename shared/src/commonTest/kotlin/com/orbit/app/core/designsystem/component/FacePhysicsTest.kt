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
    fun facesSizedForASmallStageKeepTurningClearOfTheWalls() {
        val side = 64f
        for (count in 2..8) {
            val radius = radiusFor(count, largest = 20f, width = side, height = side)
            // As big as the variety lets them get, the tightest the cluster can be.
            val faces = List(count) { FaceBody("$it", side / 2 + it, side / 2, radius * 1.1f) }
            val run = { frames: Int -> repeat(frames) { stepFaces(faces, side, side, side / 2, side / 2, step) } }
            run(600)
            val before = faces.map { it.x to it.y }

            run(60)

            assertTrue(faces.all { it.y > it.radius + 0.5f && it.y < side - it.radius - 0.5f }, "$count faces jam on the wall")
            // Seven pack as a ring around one in the middle, which has nowhere to turn to.
            assertTrue(
                faces.indices.count { hypot(faces[it].x - before[it].first, faces[it].y - before[it].second) > 1f } >= count - 1,
                "$count faces stopped turning",
            )
        }
    }

    @Test
    fun aBigStageKeepsTheSizeTheFacesAskFor() {
        assertEquals(16f, radiusFor(2, largest = 20f, width = 300f, height = 200f))
    }

    @Test
    fun eachPersonKeepsTheirOwnSize() {
        val sizes = listOf("ana", "ben", "mika", "noor").map(::sizeVariety)
        assertEquals(sizes, listOf("ana", "ben", "mika", "noor").map(::sizeVariety))
        assertTrue(sizes.all { it in 0.8f..1.1f })
        assertTrue(sizes.distinct().size > 1)
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
