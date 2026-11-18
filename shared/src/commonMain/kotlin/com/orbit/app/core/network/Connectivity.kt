package com.orbit.app.core.network

import kotlinx.coroutines.flow.Flow

/** Whether the phone has an internet connection right now, and every change after (#63). */
fun interface Connectivity {
    fun isOnline(): Flow<Boolean>
}
