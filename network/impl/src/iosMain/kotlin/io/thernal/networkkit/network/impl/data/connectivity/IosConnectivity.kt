package io.thernal.networkkit.network.impl.data.connectivity

import io.thernal.networkkit.network.api.data.connectivity.Connectivity
import io.thernal.networkkit.network.api.data.connectivity.ConnectivityMonitor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

/** The current path's status, from `NWPathMonitor`. */
fun iosConnectivityMonitor(): ConnectivityMonitor {
    return FlowConnectivityMonitor(iosConnectivity())
}

internal fun iosConnectivity(): Flow<Connectivity> {
    return callbackFlow {
        val monitor = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(monitor) { path ->
            val isSatisfied = nw_path_get_status(path) == nw_path_status_satisfied
            val status = if (isSatisfied) {
                Connectivity.Online
            } else {
                Connectivity.Offline
            }
            trySend(status)
        }
        // A null attribute is a serial queue.
        nw_path_monitor_set_queue(monitor, dispatch_queue_create("network-kit.connectivity", null))
        nw_path_monitor_start(monitor)
        awaitClose { nw_path_monitor_cancel(monitor) }
    }
}
