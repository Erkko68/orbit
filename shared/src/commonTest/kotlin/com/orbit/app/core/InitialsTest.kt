package com.orbit.app.core

import kotlin.test.Test
import kotlin.test.assertEquals

class InitialsTest {
    @Test
    fun takesTheFirstLettersOfUpToTwoWords() {
        assertEquals("AL", initialsOf("Ana López"))
        assertEquals("AL", initialsOf("  ana  lópez  garcía "))
        assertEquals("Y", initialsOf("you"))
        assertEquals("", initialsOf(""))
    }
}
