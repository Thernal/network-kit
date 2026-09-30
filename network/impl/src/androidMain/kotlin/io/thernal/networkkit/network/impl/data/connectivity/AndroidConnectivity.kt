package io.thernal.networkkit.network.impl.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.thernal.networkkit.network.api.data.connectivity.Connectivity
import io.thernal.networkkit.network.api.data.connectivity.ConnectivityMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * The default network's internet capability, from `ConnectivityManager`. Needs
 * `android.permission.ACCESS_NETWORK_STATE` in the application's manifest.
 */
fun androidConnectivityMonitor(context: Context): ConnectivityMonitor {
    return FlowConnectivityMonitor(androidConnectivity(context.applicationContext))
}

internal fun androidConnectivity(context: Context): Flow<Connectivity> {
    return callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        fun statusOf(capabilities: NetworkCapabilities?): Connectivity {
            val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            return if (isOnline) {
                Connectivity.Online
            } else {
                Connectivity.Offline
            }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                trySend(statusOf(capabilities))
            }

            override fun onLost(network: Network) {
                trySend(Connectivity.Offline)
            }
        }
        trySend(statusOf(manager.activeNetwork?.let(manager::getNetworkCapabilities)))
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }
}
