package com.orbit.app.testing

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/** A node whose tap screen readers announce as [label], such as the faces that open a space's settings. */
fun hasClickLabel(label: String) =
    SemanticsMatcher("has click label '$label'") { it.config.getOrNull(SemanticsActions.OnClick)?.label == label }
