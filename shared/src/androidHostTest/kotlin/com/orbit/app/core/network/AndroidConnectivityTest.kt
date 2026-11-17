package com.orbit.app.core.network

import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.orbit.app.core.designsystem.ComposeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetworkCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidConnectivityTest : ComposeTest() {
    private val app = RuntimeEnvironment.getApplication()
    private val manager = app.getSystemService(ConnectivityManager::class.java)
    private val connectivity = AndroidConnectivity(app)

    @Test
    fun emitsTheConnectionAndUpdatesWhenTheDefaultNetworkChanges() = runTest {
        val shadow = shadowOf(manager)
        val network = manager.activeNetwork!!
        val withoutInternet = ShadowNetworkCapabilities.newInstance()
        val unvalidated = ShadowNetworkCapabilities.newInstance().also {
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        val withInternet = ShadowNetworkCapabilities.newInstance().also {
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        shadow.setNetworkCapabilities(network, withInternet)

        val states = mutableListOf<Boolean>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            connectivity.isOnline().collect { states += it }
        }
        val callback = shadow.networkCallbacks.single()

        callback.onLost(network)
        callback.onCapabilitiesChanged(network, withoutInternet)
        callback.onCapabilitiesChanged(network, unvalidated)
        callback.onCapabilitiesChanged(network, withInternet)
        callback.onUnavailable()
        shadow.setNetworkCapabilities(network, withInternet)
        callback.onCapabilitiesChanged(network, withInternet)
        job.cancel()

        assertEquals(listOf(true, false, true, false, true), states)
        assertTrue(shadow.networkCallbacks.isEmpty())
    }

    @Test
    @Config(shadows = [ImmediateLostConnectivityShadow::class])
    fun keepsAnOfflineUpdateDeliveredWhileRegistering() = runTest {
        val shadow = shadowOf(manager)
        val network = manager.activeNetwork!!
        val withInternet = ShadowNetworkCapabilities.newInstance().also {
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            shadowOf(it).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }
        // The cached active network still looks online when the callback fires on registration.
        shadow.setNetworkCapabilities(network, withInternet)

        val states = mutableListOf<Boolean>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            connectivity.isOnline().collect { states += it }
        }
        job.cancel()

        assertEquals(listOf(false), states)
        assertTrue(shadow.networkCallbacks.isEmpty())
    }

    @Test
    fun emitsOfflineWhenConnectivityManagerIsUnavailable() = runTest {
        val withoutConnectivity = object : ContextWrapper(app) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.CONNECTIVITY_SERVICE) null else super.getSystemService(name)
        }

        assertEquals(listOf(false), AndroidConnectivity(withoutConnectivity).isOnline().toList())
    }
}

@Implements(ConnectivityManager::class)
class ImmediateLostConnectivityShadow : ShadowConnectivityManager() {
    @Implementation(minSdk = Build.VERSION_CODES.N)
    override fun registerDefaultNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback) {
        super.registerDefaultNetworkCallback(networkCallback)
        getActiveNetwork()?.let { networkCallback.onLost(it) }
    }
}
