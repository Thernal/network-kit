package io.thernal.networkkit.network.impl.data.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.thernal.networkkit.network.api.data.client.ApiClient
import io.thernal.networkkit.network.api.data.interceptor.Interceptor

/**
 * Makes the app's [ApiClient]s — one per host — over one shared HTTP client with every
 * [interceptors] installed, lowest `order` first. A presigned upload goes through a second client
 * with none of them.
 *
 * [engine] is for tests (Ktor's `MockEngine`); an app leaves it null and gets OkHttp on Android,
 * Darwin on iOS.
 */
class ApiClientFactory(
    interceptors: Set<Interceptor> = emptySet(),
    private val config: NetworkConfig = NetworkConfig(),
    engine: HttpClientEngine? = null,
) {
    private val sharedEngine: HttpClientEngine by lazy { engine ?: platformEngine() }

    private val http: HttpClient by lazy {
        HttpClient(sharedEngine) {
            expectSuccess = false
            install(ContentNegotiation) { json(config.json) }
            installTimeouts()
            interceptors.sortedBy(Interceptor::order).forEach { interceptor -> interceptor.install(client = this) }
        }
    }

    private val raw: HttpClient by lazy {
        HttpClient(sharedEngine) {
            expectSuccess = false
            installTimeouts()
        }
    }

    /**
     * A client for one host. [baseUrl] is read on every request, so an environment switched at run
     * time (a debug console's setting) applies to the next call.
     */
    fun create(baseUrl: () -> String): ApiClient {
        return KtorApiClient(http = { http }, raw = { raw }, baseUrl = baseUrl, config = config)
    }

    private fun io.ktor.client.HttpClientConfig<*>.installTimeouts() {
        val timeouts = config.timeouts ?: return
        install(HttpTimeout) {
            connectTimeoutMillis = timeouts.connect.inWholeMilliseconds
            requestTimeoutMillis = timeouts.request.inWholeMilliseconds
            socketTimeoutMillis = timeouts.socket.inWholeMilliseconds
        }
    }
}
