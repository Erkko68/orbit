package com.orbit.app.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * Motion tokens mirroring M3 Expressive springs. `MotionScheme.expressive()` is internal in stable
 * material3 1.9.0; switch to `MaterialTheme(motionScheme = ...)` once it ships stable.
 */
object OrbitMotion {
    /** Position, size and shape changes. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Color and opacity changes (no overshoot). */
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}
