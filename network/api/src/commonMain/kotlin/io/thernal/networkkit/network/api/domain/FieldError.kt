package io.thernal.networkkit.network.api.domain

data class FieldError(
    val field: String,
    val code: Int? = null,
    val error: String? = null,
)
