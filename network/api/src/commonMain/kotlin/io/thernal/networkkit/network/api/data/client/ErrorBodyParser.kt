package io.thernal.networkkit.network.api.data.client

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
