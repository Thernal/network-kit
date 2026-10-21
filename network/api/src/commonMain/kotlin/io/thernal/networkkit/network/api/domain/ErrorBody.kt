package io.thernal.networkkit.network.api.domain

import kotlinx.serialization.json.JsonElement

/**
 * A server's error body, as far as a parser could read it: an application code, its name, a
 * human-readable message, errors per field for forms, and in [extras] every other member of the body —
 * what a backend sends beside them (`"retryAfterSeconds": 900`, `"requestId": "…"`) for the app to read.
 */
data class ErrorBody(
    val code: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val fields: List<FieldError> = emptyList(),
    val extras: Map<String, JsonElement> = emptyMap(),
)
