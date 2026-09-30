package io.thernal.networkkit.network.testing

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import io.thernal.networkkit.network.api.data.client.ApiClient
import io.thernal.networkkit.network.api.data.interceptor.Interceptor
import io.thernal.networkkit.network.impl.data.client.ApiClientFactory
import io.thernal.networkkit.network.impl.data.client.NetworkConfig

/**
 * A real [ApiClient] — the same decoding, error mapping and interceptors as the app's — over Ktor's
 * `MockEngine`, answering with [handler]. For testing a feature's API class and repository without a
 * server:
 *
 * ```
 * val client = mockApiClient { request ->
 *     when (request.url.encodedPath) {
 *         "/posts" -> respondJson("""[{"id": 1}]""")
 *         else -> respondJson("""{"message": "no"}""", HttpStatusCode.NotFound)
 *     }
 * }
 * ```
 */
fun mockApiClient(
    baseUrl: String = "https://api.test",
    interceptors: Set<Interceptor> = emptySet(),
    config: NetworkConfig = NetworkConfig(),
    handler: MockRequestHandler,
): ApiClient {
    // No timeouts: under runTest's virtual time a timeout would fire before the mock answers.
    val factory = ApiClientFactory(
        interceptors = interceptors,
        config = config.copy(timeouts = null),
        engine = MockEngine(handler),
    )
    return factory.create { baseUrl }
}

fun MockRequestHandleScope.respondJson(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    headers: Map<String, String> = emptyMap(),
): HttpResponseData {
    val all = Headers.build {
        headers.forEach { (key, value) -> append(name = key, value = value) }
        append(name = HttpHeaders.ContentType, value = "application/json")
    }
    return respond(content = body, status = status, headers = all)
}
