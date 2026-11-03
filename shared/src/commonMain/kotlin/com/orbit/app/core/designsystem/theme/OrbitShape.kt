package com.orbit.app.core.designsystem.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star

private fun star(points: Int, innerRadius: Float, rounding: Float) = RoundedPolygon.star(
    numVerticesPerRadius = points,
    innerRadius = innerRadius,
    rounding = CornerRounding(rounding),
).normalized()

/**
 * The shapes of lists and chores, in the spirit of M3 Expressive's library. M3's `MaterialShapes` is alpha, so these
 * are built on the stable `graphics-shapes`. Each is a [Shape], so `Modifier.background` and `clip` take it.
 */
enum class OrbitShape(internal val polygon: RoundedPolygon) : Shape {
    Circle(RoundedPolygon.circle(numVertices = 12).normalized()),
    Squircle(RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.5f, smoothing = 1f)).normalized()),
    Cookie(star(points = 9, innerRadius = 0.86f, rounding = 0.35f)),
    Cookie12(star(points = 12, innerRadius = 0.88f, rounding = 0.3f)),
    Clover(star(points = 4, innerRadius = 0.62f, rounding = 0.55f)),
    Sunny(star(points = 8, innerRadius = 0.8f, rounding = 0.3f)),
    Burst(star(points = 12, innerRadius = 0.68f, rounding = 0.12f)),
    Flower(star(points = 6, innerRadius = 0.7f, rounding = 0.5f)),
    ;

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(Path().apply { addCubics(polygon.cubics, size) })
}

/** Appends [cubics], drawn in a unit square, scaled to [size]. */
internal fun Path.addCubics(cubics: List<Cubic>, size: Size) {
    cubics.firstOrNull()?.let { moveTo(it.anchor0X * size.width, it.anchor0Y * size.height) }
    for (cubic in cubics) {
        cubicTo(
            cubic.control0X * size.width,
            cubic.control0Y * size.height,
            cubic.control1X * size.width,
            cubic.control1Y * size.height,
            cubic.anchor1X * size.width,
            cubic.anchor1Y * size.height,
        )
    }
    close()
}

/** [Morph.asCubics] with its progress kept inside 0 to 1, for a spring that overshoots. */
internal fun Morph.cubicsAt(progress: Float): List<Cubic> = asCubics(progress.coerceIn(0f, 1f))
