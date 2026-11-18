package com.orbit.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Observes the default network through [ConnectivityManager] and reports whether it can reach the internet (#63). */
class AndroidConnectivity(private val context: Context) : Connectivity {
    override fun isOnline(): Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        if (manager == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val lock = Any()
        var reported = false
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                synchronized(lock) {
                    reported = true
                    trySend(networkCapabilities.hasInternet())
                }
            }

            override fun onLost(network: Network) {
                synchronized(lock) {
                    reported = true
                    trySend(false)
                }
            }

            override fun onUnavailable() {
                synchronized(lock) {
                    reported = true
                    trySend(false)
                }
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        synchronized(lock) {
            if (!reported) trySend(manager.getNetworkCapabilities(manager.activeNetwork).hasInternet())
        }
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

private fun NetworkCapabilities?.hasInternet(): Boolean =
    this?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
