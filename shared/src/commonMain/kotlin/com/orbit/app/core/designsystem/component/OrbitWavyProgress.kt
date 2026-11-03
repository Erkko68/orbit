package com.orbit.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.orbit.app.core.designsystem.theme.OrbitMotion
import kotlin.math.PI
import kotlin.math.sin

private val WaveHeight = 12.dp
private val WaveStroke = 3.5.dp
private val WaveAmplitude = 3.dp
private val WaveLength = 22.dp
private const val TRACK_ALPHA = 0.22f
private const val STEP_PX = 2f

/**
 * How far along something is, such as the entries ticked on a list: a wave over a straight track, in M3 Expressive's
 * manner. [progress] is a fraction from 0 to 1. A plain [Canvas], because M3's wavy indicator is alpha.
 */
@Composable
fun OrbitWavyProgress(progress: Float, modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    val shown = animateFloatAsState(progress.coerceIn(0f, 1f), OrbitMotion.spatial())
    Canvas(modifier.height(WaveHeight).progressSemantics(progress.coerceIn(0f, 1f))) {
        val middle = size.height / 2
        val inset = WaveStroke.toPx() / 2
        val width = size.width - inset * 2
        val stroke = Stroke(WaveStroke.toPx(), cap = StrokeCap.Round)
        drawLine(
            color = color.copy(alpha = TRACK_ALPHA),
            start = Offset(inset, middle),
            end = Offset(inset + width, middle),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        val end = width * shown.value
        if (end <= 0f) return@Canvas
        val wave = Path().apply {
            moveTo(inset, middle)
            var x = STEP_PX
            while (x < end) {
                lineTo(inset + x, middle + WaveAmplitude.toPx() * sin(2 * PI.toFloat() * x / WaveLength.toPx()))
                x += STEP_PX
            }
        }
        drawPath(wave, color, style = stroke)
    }
}
