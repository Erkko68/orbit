package com.orbit.app.feature.space

data class SpaceUiState(
    /** The signed-in user's spaces. Each one is drawn by a space card, which loads it by id. */
    val spaceIds: List<String> = emptyList(),
    /** What is typed in the create form. */
    val newSpaceName: String = "",
    val creating: Boolean = false,
    /** The last create failed. Cleared by the next edit or attempt. */
    val createFailed: Boolean = false,
) {
    val canCreate: Boolean get() = newSpaceName.isNotBlank() && !creating
}
