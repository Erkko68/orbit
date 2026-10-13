package com.orbit.app.core

/** Up to two initials, from the first letters of the name's words: "Ana López" is "AL". */
fun initialsOf(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
