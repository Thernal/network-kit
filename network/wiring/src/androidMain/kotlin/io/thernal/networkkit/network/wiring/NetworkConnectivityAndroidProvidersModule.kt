package io.thernal.networkkit.network.wiring

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.networkkit.network.api.data.connectivity.ConnectivityMonitor
import io.thernal.networkkit.network.impl.data.connectivity.androidConnectivityMonitor

/** Connectivity from `ConnectivityManager`. The graph provides the application `Context`. */
@BindingContainer
@ContributesTo(AppScope::class)
interface NetworkConnectivityAndroidProvidersModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideConnectivityMonitor(context: Context): ConnectivityMonitor {
            return androidConnectivityMonitor(context)
        }
    }
}
