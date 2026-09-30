package io.thernal.networkkit.network.impl.data.client

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.util.reflect.TypeInfo
import io.thernal.networkkit.network.api.data.cache.CacheValidators
import io.thernal.networkkit.network.api.data.cache.ConditionalResult
import io.thernal.networkkit.network.api.data.client.ApiClient
import io.thernal.networkkit.network.api.data.client.ApiRequest
import io.thernal.networkkit.network.api.domain.ErrorBody
import io.thernal.networkkit.network.api.domain.NetworkError
import io.thernal.networkkit.network.api.domain.NetworkException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import kotlin.coroutines.cancellation.CancellationException

/** [ApiClient] over Ktor: every outcome is a decoded body or a [NetworkException]. */
internal class KtorApiClient(
    private val http: () -> HttpClient,
    private val raw: () -> HttpClient,
    private val baseUrl: () -> String,
    private val config: NetworkConfig,
) : ApiClient {
    override suspend fun <T> send(
        request: ApiRequest,
        responseType: TypeInfo,
    ): T {
        return decode(response = sendRaw(request), type = responseType)
    }

    override suspend fun <T> sendConditional(
        request: ApiRequest,
        responseType: TypeInfo,
        validators: CacheValidators?,
    ): ConditionalResult<T> {
        val response = execute(request = request, accept = { it.isSuccess() || it == HttpStatusCode.NotModified }) {
            validators?.etag?.let { header(key = HttpHeaders.IfNoneMatch, value = it) }
            validators?.lastModified?.let { header(key = HttpHeaders.IfModifiedSince, value = it) }
        }
        if (response.status == HttpStatusCode.NotModified) {
            return ConditionalResult.NotModified
        }
        val fresh = CacheValidators(
            etag = response.headers[HttpHeaders.ETag],
            lastModified = response.headers[HttpHeaders.LastModified],
        )
        return ConditionalResult.Modified(value = decode(response = response, type = responseType), validators = fresh)
    }

    override suspend fun sendRaw(request: ApiRequest): HttpResponse {
        return execute(request = request, accept = { it.isSuccess() })
    }

    override suspend fun uploadRaw(
        url: String,
        bytes: ByteArray,
        method: HttpMethod,
        contentType: ContentType,
    ) {
        val response = transport {
            raw().request {
                this.url.takeFrom(url)
                this.method = method
                contentType(contentType)
                setBody(bytes)
            }
        }
        if (!response.status.isSuccess()) {
            throw NetworkException(statusError(response))
        }
    }

    private suspend fun execute(
        request: ApiRequest,
        accept: (HttpStatusCode) -> Boolean,
        extra: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val response = transport {
            http().request {
                configure(request)
                extra()
            }
        }
        if (!accept(response.status)) {
            throw NetworkException(statusError(response))
        }
        return response
    }

    private fun HttpRequestBuilder.configure(request: ApiRequest) {
        url {
            takeFrom(request.host ?: baseUrl())
            appendPathSegments(request.path.split('/').filter(String::isNotEmpty))
        }
        method = request.method
        request.parameters.forEach { (key, value) -> parameter(key = key, value = value) }
        request.headers.forEach { (key, value) -> header(key = key, value = value) }
        if (request.body != null) {
            contentType(ContentType.Application.Json)
            setBody(body = request.body, bodyType = checkNotNull(request.bodyType) { "A request body needs its type." })
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> decode(
        response: HttpResponse,
        type: TypeInfo,
    ): T {
        if (type.type == Unit::class) {
            return Unit as T
        }
        val text = transport { response.bodyAsText() }
        val decoded = runCatching {
            val element = config.json.parseToJsonElement(text)
            val payload = config.responseUnwrapper?.unwrap(status = response.status.value, body = element) ?: element
            val kotlinType = checkNotNull(type.kotlinType) { "No Kotlin type for ${type.type}." }
            val serializer = config.json.serializersModule.serializer(kotlinType)
            config.json.decodeFromJsonElement(deserializer = serializer, element = payload) as T
        }
        return decoded.getOrElse { failure ->
            throw failure as? NetworkException ?: NetworkException(
                error = NetworkError.Serialization(failure),
                cause = failure,
            )
        }
    }

    private suspend fun statusError(response: HttpResponse): NetworkError {
        val status = response.status.value
        return when (response.status) {
            HttpStatusCode.Unauthorized -> NetworkError.Unauthorized(errorBody(response))

            HttpStatusCode.Forbidden -> NetworkError.Forbidden(errorBody(response))

            HttpStatusCode.TooManyRequests -> NetworkError.RateLimited(
                response.headers[HttpHeaders.RetryAfter]?.toLongOrNull(),
            )

            in UNAVAILABLE -> NetworkError.Unavailable(status)

            else -> NetworkError.Http(status = status, body = errorBody(response))
        }
    }

    private suspend fun errorBody(response: HttpResponse): ErrorBody? {
        val text = try {
            response.bodyAsText()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            return null
        }
        return config.errorBodyParser.parse(status = response.status.value, body = text)
    }

    /** Runs a transport step, turning whatever the engine throws into a [NetworkException]. */
    @Suppress("TooGenericExceptionCaught") // Engines throw platform types; the unknown ones are Unexpected.
    private suspend fun <R> transport(block: suspend () -> R): R {
        return try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            throw failure as? NetworkException ?: NetworkException(error = classify(failure), cause = failure)
        }
    }

    private fun classify(failure: Exception): NetworkError {
        return when (failure) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException,
            -> NetworkError.Timeout

            is IOException -> NetworkError.NoConnection

            is SerializationException -> NetworkError.Serialization(failure)

            else -> NetworkError.Unexpected(failure)
        }
    }

    private companion object {
        val UNAVAILABLE = setOf(
            HttpStatusCode.BadGateway,
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout,
        )
    }
}
