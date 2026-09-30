package io.thernal.networkkit.network.testing

import io.thernal.networkkit.network.api.data.connectivity.Connectivity
import io.thernal.networkkit.network.api.data.connectivity.ConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow

/** A [ConnectivityMonitor] a test switches by setting [status]'s value. */
class FakeConnectivityMonitor(
    initial: Connectivity = Connectivity.Online,
) : ConnectivityMonitor {
    override val status: MutableStateFlow<Connectivity> = MutableStateFlow(initial)
}
