package com.orbit.app.core.designsystem.component

/** A plain value and its display label, with selection supplied by the caller's UI state. */
data class OrbitSelectionOption(
    val value: String,
    val label: String = value,
    val selected: Boolean = false,
)
