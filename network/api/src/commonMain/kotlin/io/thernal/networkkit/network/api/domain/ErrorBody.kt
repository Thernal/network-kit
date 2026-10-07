package io.thernal.networkkit.network.api.domain

/**
 * A server's error body, as far as a parser could read it: an application code, its name, a
 * human-readable message, and errors per field for forms.
 */
data class ErrorBody(
    val code: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val fields: List<FieldError> = emptyList(),
)
