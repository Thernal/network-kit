package io.thernal.networkkit.network.api.data.client

import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.util.reflect.TypeInfo
import io.thernal.networkkit.network.api.data.cache.CacheValidators
import io.thernal.networkkit.network.api.data.cache.ConditionalResult

/**
 * The one way features reach a server. Every call either returns the decoded body or throws a
 * [io.thernal.networkkit.network.api.domain.NetworkException] — never a raw Ktor or I/O exception.
 * Features call the verbs (`get`, `post`, …); the members here are what they are built on.
 *
 * One client talks to one base URL. An app with several hosts binds one client per host, each made
 * by `ApiClientFactory`.
 */
interface ApiClient {
    /** Sends [request] and decodes the body — unwrapped, if the app registered an unwrapper — as [responseType]. */
    suspend fun <T> send(
        request: ApiRequest,
        responseType: TypeInfo,
    ): T

    /** Like [send], but a `304 Not Modified` answer is [ConditionalResult.NotModified], not a failure. */
    suspend fun <T> sendConditional(
        request: ApiRequest,
        responseType: TypeInfo,
        validators: CacheValidators?,
    ): ConditionalResult<T>

    /** The response itself, status checked, body unread — for headers, streams, anything not JSON. */
    suspend fun sendRaw(request: ApiRequest): HttpResponse

    /**
     * Sends [bytes] to an absolute [url] — a presigned upload — through a client with no
     * interceptors: no auth header, no app headers, nothing the storage provider did not ask for.
     */
    suspend fun uploadRaw(
        url: String,
        bytes: ByteArray,
        method: HttpMethod = HttpMethod.Put,
        contentType: ContentType = ContentType.Application.OctetStream,
    )
}
