package io.thernal.networkkit.network.impl.data.connectivity

import io.thernal.networkkit.network.api.data.connectivity.Connectivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue

/** `NWPathMonitor` on the simulator, for real: it reports a path status within seconds. */
class IosConnectivityTest {
    @Test
    fun `the monitor reports a status`() {
        runBlocking {
            val status = withTimeout(PATH_REPORT_TIMEOUT_MILLIS) { iosConnectivity().first() }

            assertTrue(status == Connectivity.Online || status == Connectivity.Offline)
        }
    }
}

private const val PATH_REPORT_TIMEOUT_MILLIS = 5_000L
