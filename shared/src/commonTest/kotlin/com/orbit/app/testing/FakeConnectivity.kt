package com.orbit.app.testing

import com.orbit.app.core.network.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Test double for [Connectivity] that starts [online] and switches at will. */
class FakeConnectivity(online: Boolean = true) : Connectivity {
    private val state = MutableStateFlow(online)

    override fun isOnline(): Flow<Boolean> = state

    fun setOnline(online: Boolean) {
        state.value = online
    }
}
