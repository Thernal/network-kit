package io.thernal.networkkit.network.impl.data.client

import io.thernal.networkkit.network.api.data.client.ErrorBodyParser
import io.thernal.networkkit.network.api.domain.ErrorBody
import io.thernal.networkkit.network.api.domain.FieldError
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Reads `{ "code": 1001, "error": "VALIDATION", "message": "…", "fields": { "email": { "code": 2, "error": "TAKEN" } } }`
 * — every key optional. Anything else is not an error body: null.
 */
class ErrorBodyParserImpl(
    private val json: Json = NetworkJson,
) : ErrorBodyParser {
    override fun parse(
        status: Int,
        body: String,
    ): ErrorBody? {
        val dto = body.takeIf(String::isNotBlank)
            ?.let { text ->
                runCatching {
                    json.decodeFromString(
                        deserializer = ErrorBodyDto.serializer(),
                        string = text,
                    )
                }.getOrNull()
            }
            ?.takeUnless(ErrorBodyDto::isEmpty)
            ?: return null
        return ErrorBody(
            code = dto.code,
            error = dto.error,
            message = dto.message,
            fields = dto.fields.map { (field, failure) ->
                FieldError(
                    field = field,
                    code = failure.code,
                    error = failure.error,
                )
            },
        )
    }
}

@Serializable
private data class ErrorBodyDto(
    val code: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val fields: Map<String, FieldErrorDto> = emptyMap(),
) {
    fun isEmpty(): Boolean {
        return code == null && error == null && message == null && fields.isEmpty()
    }
}

@Serializable
private data class FieldErrorDto(
    val code: Int? = null,
    val error: String? = null,
)
