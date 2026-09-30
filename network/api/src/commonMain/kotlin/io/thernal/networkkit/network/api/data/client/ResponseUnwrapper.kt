package io.thernal.networkkit.network.api.data.client

import kotlinx.serialization.json.JsonElement

/**
 * For a backend that wraps every body in an envelope (`{ "success": …, "data": … }`): returns the
 * part to decode, or throws a [io.thernal.networkkit.network.api.domain.NetworkException] for an
 * envelope that reports failure under a success status. Without one, the body is the response.
 */
fun interface ResponseUnwrapper {
    fun unwrap(
        status: Int,
        body: JsonElement,
    ): JsonElement
}

/**
 * Reads a non-success response's body into an [io.thernal.networkkit.network.api.domain.ErrorBody],
 * or null when it is not one. The kit's default reads `{ code, error, message, fields: { name: { code, error } } }`.
 */
fun interface ErrorBodyParser {
    fun parse(
        status: Int,
        body: String,
    ): io.thernal.networkkit.network.api.domain.ErrorBody?
}
