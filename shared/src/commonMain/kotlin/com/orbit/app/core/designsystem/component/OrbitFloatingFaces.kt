package com.orbit.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitMotion
import com.orbit.app.core.designsystem.theme.OrbitShape
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.DrawableResource

private val LargestFace = 24.dp
private const val MAX_FACES = 8
private const val MAX_STEP = 0.033f
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val EMPHASIS = 1.4f
private const val SPAWN_SPREAD = 8f

/** How big a space's pool of faces is in a sheet. */
val OrbitFacePoolHeight = 300.dp

/** How big a face gets in that pool. */
val OrbitFacePoolLargest = 56.dp

/** The radius that leaves room for [count] faces, the largest of them [largest]. */
private fun radiusFor(count: Int, largest: Dp): Float = largest.value * when {
    count <= 1 -> 1f
    count <= 3 -> 0.8f
    count <= 6 -> 0.66f
    else -> 0.5f
}

/**
 * A person among [OrbitFloatingFaces]. [selected] makes them bigger, shoves the rest aside and puts a border in the
 * space's colour around them. [icon] replaces the initials on a face that stands for no one yet. [scale] sizes a face
 * against the others, such as a smaller control among the people.
 */
@Immutable
data class OrbitFloatingFace(
    val id: String,
    val initials: String,
    val color: Color,
    val shape: OrbitShape,
    val description: String,
    val photoUrl: String? = null,
    val contentColor: Color = Ink,
    val selected: Boolean = false,
    val icon: DrawableResource? = null,
    val scale: Float = 1f,
)

/**
 * Faces that float in a cluster, the group avatars of the M3 Expressive promo: they orbit a point at [centerX] and
 * [centerY], fractions of the stage, and push each other around. A face that joins pops in and shoves the others.
 * When [onFaceClick] is set they can be tapped, dragged and thrown. Up to [largest] in radius, less as they get more.
 * Shows the first [limit], for the small stages where more would not fit.
 */
@Composable
fun OrbitFloatingFaces(
    faces: List<OrbitFloatingFace>,
    modifier: Modifier = Modifier,
    largest: Dp = LargestFace,
    centerX: Float = 0.5f,
    centerY: Float = 0.5f,
    limit: Int = MAX_FACES,
    onFaceClick: ((String) -> Unit)? = null,
) {
    val density = LocalDensity.current.density
    val shown = faces.take(limit)
    val bodies = remember { mutableStateListOf<FaceBody>() }
    var stage by remember { mutableStateOf(IntSize.Zero) }
    val width = stage.width / density
    val height = stage.height / density

    LaunchedEffect(shown, width, height) {
        if (width == 0f) return@LaunchedEffect
        bodies.removeAll { body -> shown.none { it.id == body.id } }
        val radius = radiusFor(shown.size, largest)
        shown.forEachIndexed { index, face ->
            val target = (if (face.selected) radius * EMPHASIS else radius) * face.scale
            val body = bodies.firstOrNull { it.id == face.id }
            if (body == null) {
                val spread = (index - shown.size / 2f) * SPAWN_SPREAD
                bodies += FaceBody(face.id, width * centerX + spread, height * centerY + spread / 2, target)
            } else {
                body.targetRadius = target
            }
        }
    }

    LaunchedEffect(width, height, centerX, centerY) {
        var last = 0L
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                val step = if (last == 0L) 0f else ((now - last) / NANOS_PER_SECOND).coerceAtMost(MAX_STEP)
                last = now
                stepFaces(bodies, width, height, width * centerX, height * centerY, step)
            }
        }
    }

    Box(
        modifier
            .onSizeChanged { stage = it }
            .then(if (onFaceClick == null) Modifier.clearAndSetSemantics {} else Modifier),
    ) {
        for (body in bodies) {
            key(body.id) {
                shown.firstOrNull { it.id == body.id }?.let { face -> FloatingFace(face, body, onFaceClick) }
            }
        }
    }
}

@Composable
private fun FloatingFace(face: OrbitFloatingFace, body: FaceBody, onClick: ((String) -> Unit)?) {
    val density = LocalDensity.current.density
    val side = OrbitAvatarSize.Hero.size.value
    val tracker = remember { VelocityTracker() }
    val ring by animateColorAsState(
        if (face.selected) OrbitTheme.accent.vivid else Color.Transparent,
        OrbitMotion.effects(),
    )
    val touch = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .pointerInput(body) {
                detectDragGestures(
                    onDragStart = {
                        body.held = true
                        tracker.resetTracking()
                    },
                    onDragEnd = {
                        val velocity = tracker.calculateVelocity()
                        body.release(velocity.x, velocity.y)
                    },
                    onDragCancel = { body.release(0f, 0f) },
                    onDrag = { change, amount ->
                        change.consume()
                        body.x += amount.x / density
                        body.y += amount.y / density
                        tracker.addPosition(change.uptimeMillis, Offset(body.x, body.y))
                    },
                )
            }
            .semantics {
                contentDescription = face.description
                selected = face.selected
            }
            // A square ripple would show around a face of any other shape: picking it changes its size and ring instead.
            .clickable(interactionSource = null, indication = null) { onClick(face.id) }
    }
    Box(
        modifier = Modifier
            .layout { measurable, _ ->
                val pixels = (body.radius * 2 * density).roundToInt().coerceAtLeast(1)
                val placeable = measurable.measure(Constraints.fixed(pixels, pixels))
                layout(pixels, pixels) {
                    placeable.place(
                        ((body.x - body.radius) * density).roundToInt(),
                        ((body.y - body.radius) * density).roundToInt(),
                    )
                }
            }
            .then(touch),
        contentAlignment = Alignment.Center,
    ) {
        OrbitAvatar(
            initials = face.initials,
            accent = face.color,
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .graphicsLayer {
                    scaleX = body.radius * 2 / side
                    scaleY = scaleX
                    rotationZ = body.tilt
                },
            photoUrl = face.photoUrl,
            contentColor = face.contentColor,
            shape = face.shape,
            size = OrbitAvatarSize.Hero,
            icon = face.icon,
            ring = ring,
        )
    }
}
