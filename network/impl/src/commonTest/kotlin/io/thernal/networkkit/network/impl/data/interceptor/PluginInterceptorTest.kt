package io.thernal.networkkit.network.impl.data.interceptor

import io.ktor.client.plugins.api.createClientPlugin
import io.thernal.networkkit.network.api.data.client.get
import io.thernal.networkkit.network.api.data.interceptor.pluginInterceptor
import io.thernal.networkkit.network.testing.mockApiClient
import io.thernal.networkkit.network.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class HeaderConfig {
    var name: String = "X-Default"
    var value: String = ""
}

/** A plugin with a configuration of its own, the shape of a debug console's or a logger's. */
private val HeaderPlugin = createClientPlugin("TestHeader", ::HeaderConfig) {
    val name = pluginConfig.name
    val value = pluginConfig.value
    onRequest { request, _ -> request.headers.append(name, value) }
}

class PluginInterceptorTest {
    @Test
    fun aConfiguredPluginIsInstalledWithItsConfiguration() {
        runTest {
            var seen: String? = null
            val interceptor = pluginInterceptor(HeaderPlugin, order = 200) {
                name = "X-Trace"
                value = "abc"
            }
            val client = mockApiClient(interceptors = setOf(interceptor)) { request ->
                seen = request.headers["X-Trace"]
                respondJson("{}")
            }

            client.get<Unit>("/ping")

            assertEquals(expected = "abc", actual = seen)
            assertEquals(expected = 200, actual = interceptor.order)
        }
    }

    @Test
    fun aPluginTakenAsItIsKeepsItsDefaults() {
        runTest {
            var seen: String? = null
            val client = mockApiClient(interceptors = setOf(pluginInterceptor(HeaderPlugin))) { request ->
                seen = request.headers["X-Default"]
                respondJson("{}")
            }

            client.get<Unit>("/ping")

            assertEquals(expected = "", actual = seen)
        }
    }
}
