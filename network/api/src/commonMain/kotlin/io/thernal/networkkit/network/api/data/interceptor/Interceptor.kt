package io.thernal.networkkit.network.api.data.interceptor

import io.ktor.client.plugins.api.ClientPlugin

/**
 * A Ktor client plugin installed into every client `ApiClientFactory` makes — auth, logging, app
 * headers, a debug console. Lower [order] is installed first.
 */
interface Interceptor {
    val order: Int get() = DEFAULT_ORDER

    val plugin: ClientPlugin<Unit>

    companion object {
        const val DEFAULT_ORDER: Int = 100
    }
}
