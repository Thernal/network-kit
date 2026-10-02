package io.thernal.networkkit.network.api.data.interceptor

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.ClientPlugin

/**
 * Something installed into every client `ApiClientFactory` makes — auth, logging, app headers, a debug
 * console. Lower [order] is installed first.
 *
 * [install] receives the client being built, so any Ktor plugin fits, configured or not:
 * `client.install(plugin) { … }`. For a plugin taken as it is, [pluginInterceptor] is the one-liner.
 */
interface Interceptor {
    val order: Int get() = DEFAULT_ORDER

    fun install(client: HttpClientConfig<*>)

    companion object {
        const val DEFAULT_ORDER: Int = 100
    }
}

/**
 * An [Interceptor] for any Ktor client plugin, whatever its configuration type [C] — a debug console's,
 * a logger's: `pluginInterceptor(ConsolePlugin, order = 200) { maskHeaders = true }`.
 */
fun <C : Any> pluginInterceptor(
    plugin: ClientPlugin<C>,
    order: Int = Interceptor.DEFAULT_ORDER,
    configure: C.() -> Unit = {},
): Interceptor {
    return PluginInterceptor(plugin = plugin, order = order, configure = configure)
}

private class PluginInterceptor<C : Any>(
    private val plugin: ClientPlugin<C>,
    override val order: Int,
    private val configure: C.() -> Unit,
) : Interceptor {
    override fun install(client: HttpClientConfig<*>) {
        client.install(plugin = plugin, configure = configure)
    }
}
