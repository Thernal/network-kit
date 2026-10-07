package io.thernal.networkkit.network.api.data.cache

import io.ktor.util.reflect.typeInfo
import io.thernal.networkkit.network.api.data.client.ApiClient
import io.thernal.networkkit.network.api.data.client.RequestBuilder

/**
 * GETs [path] conditionally: sends the validators [store] has for [cacheKey]; returns the new body
 * (and stores its validators), or null when the server answers `304 Not Modified` — the caller keeps
 * what it already has.
 */
suspend inline fun <reified T> ApiClient.fetchConditional(
    store: HttpCacheStore,
    path: String,
    cacheKey: String = path,
    block: RequestBuilder.() -> Unit = {},
): T? {
    val request = RequestBuilder(method = io.ktor.http.HttpMethod.Get, path = path).apply(block).build()
    val result = sendConditional<T>(request = request, responseType = typeInfo<T>(), validators = store.get(cacheKey))
    if (result !is ConditionalResult.Modified) {
        return null
    }
    store.save(key = cacheKey, validators = result.validators)
    return result.value
}
