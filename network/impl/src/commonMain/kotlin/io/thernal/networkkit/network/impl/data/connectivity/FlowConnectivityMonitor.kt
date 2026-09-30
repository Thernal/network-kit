package io.thernal.networkkit.network.impl.data.connectivity

import io.thernal.networkkit.network.api.data.connectivity.Connectivity
import io.thernal.networkkit.network.api.data.connectivity.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/**
 * A [ConnectivityMonitor] over a platform's callback [updates], collected from creation for the life of
 * [scope] — one per app. `androidConnectivityMonitor(context)` and `iosConnectivityMonitor()` build it.
 */
class FlowConnectivityMonitor(
    updates: Flow<Connectivity>,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ConnectivityMonitor {
    override val status: StateFlow<Connectivity> = updates
        .distinctUntilChanged()
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = Connectivity.Unknown)
}
