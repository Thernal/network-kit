package io.thernal.networkkit.network.impl.data.client

import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.thernal.networkkit.network.api.data.cache.CacheValidators
import io.thernal.networkkit.network.api.data.cache.fetchConditional
import io.thernal.networkkit.network.api.data.client.ResponseUnwrapper
import io.thernal.networkkit.network.api.data.client.delete
import io.thernal.networkkit.network.api.data.client.get
import io.thernal.networkkit.network.api.data.client.post
import io.thernal.networkkit.network.api.domain.ErrorBody
import io.thernal.networkkit.network.api.domain.FieldError
import io.thernal.networkkit.network.api.domain.NetworkError
import io.thernal.networkkit.network.api.domain.NetworkException
import io.thernal.networkkit.network.api.domain.NetworkResult
import io.thernal.networkkit.network.api.domain.networkCall
import io.thernal.networkkit.network.impl.data.cache.InMemoryHttpCacheStore
import io.thernal.networkkit.network.testing.mockApiClient
import io.thernal.networkkit.network.testing.respondJson
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

@Serializable
private data class Post(
    val id: Int,
    val title: String = "",
)

@Serializable
private data class NewPost(
    val title: String,
)

class KtorApiClientTest {
    @Test
    fun `get joins the base url path and parameters and decodes`() {
        runTest {
            var seen = ""
            val client = mockApiClient(baseUrl = "https://api.test/v1/") { request ->
                seen = request.url.toString()
                respondJson("""{"id": 1, "title": "Hi", "extra": true}""")
            }

            val post: Post = client.get("/posts/1") { parameter("lang", "az") }

            assertEquals(expected = Post(id = 1, title = "Hi"), actual = post)
            assertEquals(expected = "https://api.test/v1/posts/1?lang=az", actual = seen)
        }
    }

    @Test
    fun `post serializes the body as json`() {
        runTest {
            var sent = ""
            val client = mockApiClient { request ->
                sent = request.body.toByteArray().decodeToString()
                respondJson("""{"id": 2, "title": "New"}""", HttpStatusCode.Created)
            }

            val created: Post = client.post("/posts", NewPost(title = "New"))

            assertEquals(expected = """{"title":"New"}""", actual = sent)
            assertEquals(expected = 2, actual = created.id)
        }
    }

    @Test
    fun `a unit response reads no body`() {
        runTest {
            val client = mockApiClient { respond(content = "", status = HttpStatusCode.NoContent) }

            client.delete<Unit>("/posts/1")
        }
    }

    @Test
    fun `statuses become their errors`() {
        runTest {
            suspend fun errorFor(
                status: HttpStatusCode,
                body: String = "",
                headers: Map<String, String> = emptyMap(),
            ): NetworkError {
                val client = mockApiClient { respondJson(body, status, headers) }
                return assertFailsWith<NetworkException> { client.get<Post>("/x") }.error
            }

            assertEquals(expected = NetworkError.Unauthorized(), actual = errorFor(HttpStatusCode.Unauthorized))
            assertEquals(expected = NetworkError.Forbidden(), actual = errorFor(HttpStatusCode.Forbidden))
            assertEquals(
                expected = NetworkError.RateLimited(retryAfterSeconds = 30),
                actual = errorFor(HttpStatusCode.TooManyRequests, headers = mapOf(HttpHeaders.RetryAfter to "30")),
            )
            assertEquals(expected = NetworkError.Unavailable(503), actual = errorFor(HttpStatusCode.ServiceUnavailable))
            assertEquals(
                expected = NetworkError.Http(status = 404, body = null),
                actual = errorFor(HttpStatusCode.NotFound, "<html>"),
            )
        }
    }

    @Test
    fun `an error body is parsed with its field errors`() {
        runTest {
            val body = """
                {"code": 1001, "error": "VALIDATION", "message": "Check the form",
                 "fields": {"email": {"code": 2, "error": "TAKEN"}}}
            """.trimIndent()
            val client = mockApiClient { respondJson(body, HttpStatusCode.UnprocessableEntity) }

            val error = assertFailsWith<NetworkException> { client.post<NewPost, Post>("/posts", NewPost("x")) }.error

            val expected = ErrorBody(
                code = 1001,
                error = "VALIDATION",
                message = "Check the form",
                fields = listOf(FieldError(field = "email", code = 2, error = "TAKEN")),
            )
            assertEquals(expected = NetworkError.Http(status = 422, body = expected), actual = error)
        }
    }

    @Test
    fun `a body of the wrong shape is a serialization error`() {
        runTest {
            val client = mockApiClient { respondJson("""{"title": "no id"}""") }

            val error = assertFailsWith<NetworkException> { client.get<Post>("/x") }.error

            assertIs<NetworkError.Serialization>(error)
        }
    }

    @Test
    fun `an engine failure is no connection and network call returns it`() {
        runTest {
            val client = mockApiClient { throw IOException("offline") }

            val result = networkCall { client.get<Post>("/x") }

            assertEquals(expected = NetworkResult.Failure(NetworkError.NoConnection), actual = result)
        }
    }

    @Test
    fun `an unwrapper opens the envelope`() {
        runTest {
            val unwrapper = ResponseUnwrapper { _, body -> checkNotNull(body.jsonObject["data"]) }
            val client = mockApiClient(config = NetworkConfig(responseUnwrapper = unwrapper)) {
                respondJson("""{"success": true, "data": {"id": 3}}""")
            }

            assertEquals(expected = 3, actual = client.get<Post>("/x").id)
            assertIs<JsonObject>(client.get<JsonObject>("/x"))
        }
    }

    @Test
    fun `the base url is read on every request`() {
        runTest {
            var base = "https://one.test"
            val hosts = mutableListOf<String>()
            val client = ApiClientFactory(
                config = NetworkConfig(timeouts = null),
                engine = io.ktor.client.engine.mock.MockEngine { request ->
                    hosts += request.url.host
                    respondJson("""{"id": 1}""")
                },
            ).create { base }

            client.get<Post>("/x")
            base = "https://two.test"
            client.get<Post>("/x")
            client.get<Post>("/x") { host("https://three.test") }

            assertEquals(expected = listOf("one.test", "two.test", "three.test"), actual = hosts)
        }
    }

    @Test
    fun `a conditional fetch sends validators and returns null on not modified`() {
        runTest {
            val store = InMemoryHttpCacheStore()
            val sent = mutableListOf<String?>()
            val client = mockApiClient { request ->
                sent += request.headers[HttpHeaders.IfNoneMatch]
                if (request.headers[HttpHeaders.IfNoneMatch] == "\"v1\"") {
                    respond(content = "", status = HttpStatusCode.NotModified)
                } else {
                    respondJson("""{"id": 1}""", headers = mapOf(HttpHeaders.ETag to "\"v1\""))
                }
            }

            assertEquals(expected = 1, actual = client.fetchConditional<Post>(store, "/config")?.id)
            assertEquals(
                expected = CacheValidators(etag = "\"v1\"", lastModified = null),
                actual = store.get("/config"),
            )
            assertNull(client.fetchConditional<Post>(store, "/config"))
            assertEquals(expected = listOf(null, "\"v1\""), actual = sent)
        }
    }

    @Test
    fun `an upload goes out bare and reports failure`() {
        runTest {
            var method: HttpMethod? = null
            val client = mockApiClient { request ->
                method = request.method
                if (request.url.host == "bucket.test") {
                    respond(content = "", status = HttpStatusCode.OK)
                } else {
                    respond(content = "", status = HttpStatusCode.Forbidden)
                }
            }

            client.uploadRaw("https://bucket.test/object?sig=1", byteArrayOf(1, 2))
            assertEquals(expected = HttpMethod.Put, actual = method)
            val error = assertFailsWith<NetworkException> {
                client.uploadRaw(
                    "https://other.test/o",
                    byteArrayOf(1),
                )
            }.error
            assertIs<NetworkError.Forbidden>(error)
        }
    }
}
