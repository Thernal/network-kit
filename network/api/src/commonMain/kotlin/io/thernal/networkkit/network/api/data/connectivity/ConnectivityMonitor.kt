package io.thernal.networkkit.network.api.data.connectivity

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the device can reach the internet, as the platform reports it: Android's
 * `ConnectivityManager`, iOS's `NWPathMonitor`. For showing an offline banner, pausing sync, or letting
 * the retry interceptor give up at once instead of backing off against no network.
 *
 * "Online" means a route exists, not that a server answers: a call can still fail with `NoConnection`.
 */
interface ConnectivityMonitor {
    val status: StateFlow<Connectivity>
}
