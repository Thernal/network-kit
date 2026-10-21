package io.thernal.networkkit.network.impl.data.client

import io.thernal.networkkit.network.api.data.client.ErrorBodyParser
import io.thernal.networkkit.network.api.domain.ErrorBody
import io.thernal.networkkit.network.api.domain.FieldError
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Reads `{ "code": 1001, "error": "VALIDATION", "message": "…", "fields": { "email": { "code": 2, "error": "TAKEN" } } }`
 * — every key optional — and keeps every other member in [ErrorBody.extras]; a body whose known keys have
 * another shape is kept whole there. Anything but a non-empty JSON object is not an error body: null.
 */
class ErrorBodyParserImpl(
    private val json: Json = NetworkJson,
) : ErrorBodyParser {
    override fun parse(
        status: Int,
        body: String,
    ): ErrorBody? {
        val members = body.takeIf(String::isNotBlank)
            ?.let { text -> runCatching { json.parseToJsonElement(text) }.getOrNull() }
            as? JsonObject
            ?: return null
        val dto = runCatching {
            json.decodeFromJsonElement(
                deserializer = ErrorBodyDto.serializer(),
                element = members,
            )
        }.getOrNull()
        val extras = if (dto == null) {
            members
        } else {
            members - READ_KEYS
        }
        if ((dto == null || dto.isEmpty()) && extras.isEmpty()) {
            return null
        }
        return ErrorBody(
            code = dto?.code,
            error = dto?.error,
            message = dto?.message,
            fields = dto?.fields.orEmpty().map { (field, failure) ->
                FieldError(
                    field = field,
                    code = failure.code,
                    error = failure.error,
                )
            },
            extras = extras,
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

private val READ_KEYS = setOf("code", "error", "message", "fields")
