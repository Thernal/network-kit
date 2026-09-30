package io.thernal.networkkit.network.impl.data.retry

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.thernal.networkkit.network.api.data.client.ApiClient
import io.thernal.networkkit.network.api.data.client.get
import io.thernal.networkkit.network.api.data.client.post
import io.thernal.networkkit.network.api.data.connectivity.Connectivity
import io.thernal.networkkit.network.api.domain.NetworkError
import io.thernal.networkkit.network.api.domain.NetworkException
import io.thernal.networkkit.network.testing.FakeConnectivityMonitor
import io.thernal.networkkit.network.testing.mockApiClient
import io.thernal.networkkit.network.testing.respondJson
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val RETRY_AFTER_MILLIS = 3_000L

class RetryInterceptorTest {
    private val connectivity = FakeConnectivityMonitor()
    private val retry = RetryInterceptor(connectivity = connectivity, random = Random(seed = 1))
    private var calls = 0

    private fun serverAnswering(
        vararg statuses: HttpStatusCode,
    ): ApiClient {
        return mockApiClient(interceptors = setOf(retry)) {
            val status = statuses.getOrElse(calls) { statuses.last() }
            calls++
            respondJson("{}", status)
        }
    }

    @Test
    fun anIdempotentRequestIsRetriedUntilItSucceeds() {
        runTest {
            val client = serverAnswering(
                HttpStatusCode.ServiceUnavailable,
                HttpStatusCode.BadGateway,
                HttpStatusCode.OK,
            )

            client.get<JsonObject>("/x")

            assertEquals(expected = 3, actual = calls)
        }
    }

    @Test
    fun retriesAreBounded() {
        runTest {
            val client = serverAnswering(HttpStatusCode.ServiceUnavailable)

            val error = assertFailsWith<NetworkException> { client.get<JsonObject>("/x") }.error

            assertEquals(expected = NetworkError.Unavailable(503), actual = error)
            assertEquals(expected = 3, actual = calls)
        }
    }

    @Test
    fun postIsNeverRetried() {
        runTest {
            val client = serverAnswering(HttpStatusCode.ServiceUnavailable, HttpStatusCode.OK)

            assertFailsWith<NetworkException> { client.post<JsonObject>("/x") }

            assertEquals(expected = 1, actual = calls)
        }
    }

    @Test
    fun aStatusThatRetryingCannotFixIsNotRetried() {
        runTest {
            val client = serverAnswering(HttpStatusCode.NotFound, HttpStatusCode.OK)

            assertFailsWith<NetworkException> { client.get<JsonObject>("/x") }

            assertEquals(expected = 1, actual = calls)
        }
    }

    @Test
    fun aDroppedConnectionIsRetried() {
        runTest {
            val client = mockApiClient(interceptors = setOf(retry)) {
                calls++
                if (calls == 1) {
                    throw IOException("reset")
                } else {
                    respondJson("{}")
                }
            }

            client.get<JsonObject>("/x")

            assertEquals(expected = 2, actual = calls)
        }
    }

    @Test
    fun whileOfflineTheFailureComesBackAtOnce() {
        runTest {
            connectivity.status.value = Connectivity.Offline
            val client = mockApiClient(interceptors = setOf(retry)) {
                calls++
                throw IOException("offline")
            }

            val error = assertFailsWith<NetworkException> { client.get<JsonObject>("/x") }.error

            assertEquals(expected = NetworkError.NoConnection, actual = error)
            assertEquals(expected = 1, actual = calls)
        }
    }

    @Test
    fun retryAfterIsHonouredAndALongOneIsNotWaitedFor() {
        runTest {
            val client = mockApiClient(interceptors = setOf(retry)) { request ->
                calls++
                val wait = if (request.url.encodedPath == "/long") {
                    "600"
                } else {
                    "3"
                }
                if (calls == 1 || request.url.encodedPath == "/long") {
                    respondJson("{}", HttpStatusCode.TooManyRequests, mapOf(HttpHeaders.RetryAfter to wait))
                } else {
                    respondJson("{}")
                }
            }

            client.get<JsonObject>("/short")
            assertTrue(currentTime >= RETRY_AFTER_MILLIS)

            val error = assertFailsWith<NetworkException> { client.get<JsonObject>("/long") }.error
            assertEquals(expected = NetworkError.RateLimited(retryAfterSeconds = 600), actual = error)
        }
    }
}
