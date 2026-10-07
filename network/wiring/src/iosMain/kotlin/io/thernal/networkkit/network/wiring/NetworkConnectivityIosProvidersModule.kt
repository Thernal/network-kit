package io.thernal.networkkit.network.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.networkkit.network.api.data.connectivity.ConnectivityMonitor
import io.thernal.networkkit.network.impl.data.connectivity.iosConnectivityMonitor

/** Connectivity from `NWPathMonitor`. */
@BindingContainer
@ContributesTo(AppScope::class)
interface NetworkConnectivityIosProvidersModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideConnectivityMonitor(): ConnectivityMonitor {
            return iosConnectivityMonitor()
        }
    }
}
