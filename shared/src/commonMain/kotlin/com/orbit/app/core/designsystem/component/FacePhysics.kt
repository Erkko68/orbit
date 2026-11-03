package com.orbit.app.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot

private const val PULL = 7f
/** How fast the cluster turns around its centre, in radians a second: a lap in about 12 seconds. */
private const val ORBIT_RATE = 0.5f
private const val DRAG = 3f
private const val BOUNCE = 0.6f
private const val COLLISION = 1.6f
private const val GROW_STIFFNESS = 180f
private const val GROW_DAMPING = 16f
private const val FACE_GAP = 2f
private const val MAX_THROW = 1400f
private const val TILT = 0.03f
private const val MAX_TILT = 14f

/** One face in free fall around a point, in dp. Read by the layout, so a frame moves it without recomposing. */
internal class FaceBody(val id: String, x: Float, y: Float, var targetRadius: Float) {
    var x by mutableFloatStateOf(x)
    var y by mutableFloatStateOf(y)
    var radius by mutableFloatStateOf(1f)
    var tilt by mutableFloatStateOf(0f)
    var vx = 0f
    var vy = 0f
    var growth = 0f
    var held = false

    /** Lets go of a held face with the speed of the finger. */
    fun release(velocityX: Float, velocityY: Float) {
        held = false
        vx = velocityX.coerceIn(-MAX_THROW, MAX_THROW)
        vy = velocityY.coerceIn(-MAX_THROW, MAX_THROW)
    }
}

/**
 * One step of [dt] seconds in a [width] by [height] stage. Each face is pulled to the centre, so they gather in a
 * cluster, and the cluster keeps turning around it. They bounce off the walls and each other. A held face moves nothing but pushes the rest.
 */
internal fun stepFaces(bodies: List<FaceBody>, width: Float, height: Float, centerX: Float, centerY: Float, dt: Float) {
    for (body in bodies) {
        body.growth += (GROW_STIFFNESS * (body.targetRadius - body.radius) - GROW_DAMPING * body.growth) * dt
        body.radius += body.growth * dt
        if (!body.held) body.fall(width, height, centerX, centerY, dt)
    }
    for (first in bodies.indices) {
        for (second in first + 1..bodies.lastIndex) collide(bodies[first], bodies[second])
    }
}

private fun FaceBody.fall(width: Float, height: Float, centerX: Float, centerY: Float, dt: Float) {
    val toX = centerX - x
    val toY = centerY - y
    // Drag takes away only what differs from turning around the centre, so the cluster keeps orbiting it.
    val orbitVx = toY * ORBIT_RATE
    val orbitVy = -toX * ORBIT_RATE
    val drag = exp(-DRAG * dt)
    vx = orbitVx + (vx - orbitVx + toX * PULL * dt) * drag
    vy = orbitVy + (vy - orbitVy + toY * PULL * dt) * drag
    x += vx * dt
    y += vy * dt
    tilt = (vx * TILT).coerceIn(-MAX_TILT, MAX_TILT)
    val right = (width - radius).coerceAtLeast(radius)
    val bottom = (height - radius).coerceAtLeast(radius)
    if (x < radius) {
        x = radius
        vx = abs(vx) * BOUNCE
    } else if (x > right) {
        x = right
        vx = -abs(vx) * BOUNCE
    }
    if (y < radius) {
        y = radius
        vy = abs(vy) * BOUNCE
    } else if (y > bottom) {
        y = bottom
        vy = -abs(vy) * BOUNCE
    }
}

private fun collide(a: FaceBody, b: FaceBody) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val apart = a.radius + b.radius + FACE_GAP
    val distance = hypot(dx, dy)
    if (distance >= apart) return
    // Two faces exactly on top of each other have no direction, so they part sideways.
    val normalX = if (distance == 0f) 1f else dx / distance
    val normalY = if (distance == 0f) 0f else dy / distance
    val overlap = apart - distance
    val shareA = if (a.held) 0f else if (b.held) 1f else 0.5f
    val shareB = if (b.held) 0f else if (a.held) 1f else 0.5f
    a.x -= normalX * overlap * shareA
    a.y -= normalY * overlap * shareA
    b.x += normalX * overlap * shareB
    b.y += normalY * overlap * shareB
    val closing = (b.vx - a.vx) * normalX + (b.vy - a.vy) * normalY
    if (closing >= 0f) return
    val impulse = -closing * COLLISION / 2
    if (!a.held) {
        a.vx -= impulse * normalX
        a.vy -= impulse * normalY
    }
    if (!b.held) {
        b.vx += impulse * normalX
        b.vy += impulse * normalY
    }
}
