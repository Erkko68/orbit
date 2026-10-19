package com.orbit.app.core.notification.permission

/** Asking the user to allow notifications, for a feature that just needed them. */
data class NotificationPermissionUiState(
    /** Notifications are off and the system prompt has not been shown yet. */
    val prompting: Boolean = false,
    /** The user refused in the prompt: the dialog points to the system settings, the only way left. */
    val refused: Boolean = false,
    /** Whether notifications ended up allowed, once that is settled. */
    val result: Boolean? = null,
)
