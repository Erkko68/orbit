package com.orbit.app.core.designsystem.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class OrbitColorsTest {
    @Test
    fun spaceAccentWrapsAroundTheAccents() {
        val accents = LightOrbitColors.spaceAccents

        assertEquals(accents.first(), LightOrbitColors.spaceAccent(0))
        assertEquals(accents[1], LightOrbitColors.spaceAccent(accents.size + 1))
        assertEquals(accents.last(), LightOrbitColors.spaceAccent(-1))
    }
}
