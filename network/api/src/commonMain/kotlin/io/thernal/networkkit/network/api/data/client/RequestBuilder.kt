package io.thernal.networkkit.network.api.data.client

import io.ktor.http.HttpMethod
import io.ktor.util.reflect.TypeInfo

/** Builds an [ApiRequest]; the verb functions hand one to their `block`. */
class RequestBuilder(
    private val method: HttpMethod,
    private val path: String,
) {
    private var host: String? = null
    private val parameters = linkedMapOf<String, String>()
    private val headers = linkedMapOf<String, String>()
    private var body: Any? = null
    private var bodyType: TypeInfo? = null

    /** Sends this one request to another host than the client's base URL. */
    fun host(value: String) {
        host = value
    }

    fun parameter(
        key: String,
        value: Any?,
    ) {
        if (value != null) {
            parameters[key] = value.toString()
        }
    }

    fun header(
        key: String,
        value: String,
    ) {
        headers[key] = value
    }

    fun body(
        value: Any,
        type: TypeInfo,
    ) {
        body = value
        bodyType = type
    }

    fun build(): ApiRequest {
        return ApiRequest(
            method = method,
            path = path,
            host = host,
            parameters = parameters.toMap(),
            headers = headers.toMap(),
            body = body,
            bodyType = bodyType,
        )
    }
}
