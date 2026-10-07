package io.thernal.networkkit.network.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.networkkit.network.api.data.client.ErrorBodyParser
import io.thernal.networkkit.network.api.data.client.ResponseUnwrapper
import io.thernal.networkkit.network.api.data.interceptor.Interceptor
import io.thernal.networkkit.network.impl.data.client.ApiClientFactory
import io.thernal.networkkit.network.impl.data.client.ErrorBodyParserImpl
import io.thernal.networkkit.network.impl.data.client.NetworkConfig

/**
 * The client factory, with every contributed [Interceptor]. An app makes its `ApiClient`s from the
 * factory — one per host, since only the app knows its base URLs:
 *
 * ```
 * @Provides @SingleIn(AppScope::class)
 * fun apiClient(factory: ApiClientFactory): ApiClient = factory.create { Environment.BASE_URL }
 * ```
 *
 * An envelope backend contributes one [ResponseUnwrapper] into its set; a different error body shape,
 * one [ErrorBodyParser]. Both sets are empty by default.
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface NetworkProvidersModule {
    @Multibinds(allowEmpty = true)
    val interceptors: Set<Interceptor>

    @Multibinds(allowEmpty = true)
    val responseUnwrappers: Set<ResponseUnwrapper>

    @Multibinds(allowEmpty = true)
    val errorBodyParsers: Set<ErrorBodyParser>

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideApiClientFactory(
            interceptors: Set<Interceptor>,
            responseUnwrappers: Set<ResponseUnwrapper>,
            errorBodyParsers: Set<ErrorBodyParser>,
        ): ApiClientFactory {
            val config = NetworkConfig(
                responseUnwrapper = responseUnwrappers.singleOrNull(),
                errorBodyParser = errorBodyParsers.singleOrNull() ?: ErrorBodyParserImpl(),
            )
            return ApiClientFactory(interceptors = interceptors, config = config)
        }
    }
}
