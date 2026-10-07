package io.thernal.networkkit.network.api.data.client

import io.ktor.http.HttpMethod
import io.ktor.util.reflect.TypeInfo

/**
 * One request, relative to its client's base URL unless [host] overrides it. The body is serialized
 * as JSON by its [bodyType].
 */
data class ApiRequest(
    val method: HttpMethod,
    val path: String,
    val host: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val headers: Map<String, String> = emptyMap(),
    val body: Any? = null,
    val bodyType: TypeInfo? = null,
)
