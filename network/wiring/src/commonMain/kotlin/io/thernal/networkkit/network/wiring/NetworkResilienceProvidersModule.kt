package io.thernal.networkkit.network.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.networkkit.network.api.data.connectivity.ConnectivityMonitor
import io.thernal.networkkit.network.api.data.interceptor.Interceptor
import io.thernal.networkkit.network.impl.data.retry.RetryInterceptor

/**
 * Automatic retries for idempotent requests, giving up at once while offline. The
 * [ConnectivityMonitor] comes from `NetworkConnectivityAndroidProvidersModule` / `NetworkConnectivityIosProvidersModule`.
 * An app that wants neither excludes these three containers from its graph.
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface NetworkResilienceProvidersModule {
    companion object {
        @Provides
        @IntoSet
        fun provideRetryInterceptor(connectivity: ConnectivityMonitor): Interceptor {
            return RetryInterceptor(connectivity = connectivity)
        }
    }
}
