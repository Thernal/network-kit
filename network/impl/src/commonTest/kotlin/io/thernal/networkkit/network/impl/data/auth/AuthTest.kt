package io.thernal.networkkit.network.impl.data.auth

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.thernal.networkkit.network.api.data.auth.ApiRoutePattern
import io.thernal.networkkit.network.api.data.auth.AuthRoutes
import io.thernal.networkkit.network.api.data.auth.SessionState
import io.thernal.networkkit.network.api.data.auth.Tokens
import io.thernal.networkkit.network.api.data.client.ApiClient
import io.thernal.networkkit.network.api.data.client.get
import io.thernal.networkkit.network.api.domain.NetworkError
import io.thernal.networkkit.network.api.domain.NetworkException
import io.thernal.networkkit.network.testing.FakeTokenRefresher
import io.thernal.networkkit.network.testing.FakeTokenStore
import io.thernal.networkkit.network.testing.mockApiClient
import io.thernal.networkkit.network.testing.respondJson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AuthTest {
    private val store = FakeTokenStore()
    private val refresher = FakeTokenRefresher()
    private val session = SessionManagerImpl(store, refresher)
    private val routes = AuthRoutes(
        public = setOf(ApiRoutePattern("POST", "/auth/refresh"), ApiRoutePattern(null, "/auth/login")),
        optional = setOf(ApiRoutePattern("GET", "/feed")),
    )
    private val seenTokens = mutableListOf<String?>()

    /** A server that accepts exactly [valid] as the bearer token. */
    private fun server(valid: () -> String?): ApiClient {
        return mockApiClient(interceptors = setOf(AuthInterceptor(session, setOf(routes)))) { request ->
            val auth = request.headers[HttpHeaders.Authorization]
            seenTokens += auth
            val isPublic = request.url.encodedPath == "/auth/login"
            val isGuestFeed = request.url.encodedPath == "/feed" && auth == null
            val isValid = valid()?.let { auth == "Bearer $it" } ?: false
            if (isPublic || isGuestFeed || isValid) {
                respondJson("{}")
            } else {
                respondJson("{}", HttpStatusCode.Unauthorized)
            }
        }
    }

    @Test
    fun `the token goes to protected routes but not public ones`() {
        runTest {
            session.signIn(Tokens(accessToken = "a1", refreshToken = "r1"))
            val client = server { "a1" }

            client.get<JsonObject>("/me")
            client.get<JsonObject>("/auth/login")

            assertEquals(expected = listOf("Bearer a1", null), actual = seenTokens)
        }
    }

    @Test
    fun `a guest sends no token and a 401 refreshes nothing`() {
        runTest {
            val client = server { "a1" }

            val error = assertFailsWith<NetworkException> { client.get<JsonObject>("/me") }.error

            assertEquals(expected = NetworkError.Unauthorized(), actual = error)
            assertEquals(expected = listOf<String?>(null), actual = seenTokens)
            assertEquals(expected = 0, actual = refresher.calls)
            assertEquals(expected = SessionState.Guest, actual = session.state.value)
        }
    }

    @Test
    fun `concurrent 401 s share one refresh and retry with the new token`() {
        runTest {
            session.signIn(Tokens(accessToken = "old", refreshToken = "r1"))
            var valid = "new"
            refresher.renew = {
                delay(REFRESH_DELAY_MILLIS)
                Tokens(accessToken = "new", refreshToken = "r2")
            }
            val client = server { valid }

            (1..CONCURRENT_REQUESTS).map { async { client.get<JsonObject>("/me") } }.awaitAll()

            assertEquals(expected = 1, actual = refresher.calls)
            assertEquals(expected = Tokens(accessToken = "new", refreshToken = "r2"), actual = store.tokens)
            assertEquals(expected = SessionState.Authenticated, actual = session.state.value)
            valid = "unused"
        }
    }

    @Test
    fun `a rejected refresh expires the session`() {
        runTest {
            session.signIn(Tokens(accessToken = "old", refreshToken = "r1"))
            refresher.renew = { null }
            val client = server { "never" }

            val error = assertFailsWith<NetworkException> { client.get<JsonObject>("/me") }.error

            assertEquals(expected = NetworkError.Unauthorized(), actual = error)
            assertEquals(expected = SessionState.Expired, actual = session.state.value)
            assertNull(store.tokens)
        }
    }

    @Test
    fun `a refresh that cannot be tried keeps the session`() {
        runTest {
            session.signIn(Tokens(accessToken = "old", refreshToken = "r1"))
            refresher.renew = { throw NetworkException(NetworkError.NoConnection) }
            val client = server { "never" }

            assertFailsWith<NetworkException> { client.get<JsonObject>("/me") }

            assertEquals(expected = SessionState.Authenticated, actual = session.state.value)
            assertEquals(expected = "old", actual = store.tokens?.accessToken)
        }
    }

    @Test
    fun `a refresh endpoint that answers 401 ends the session`() {
        runTest {
            session.signIn(Tokens(accessToken = "old", refreshToken = "r1"))
            refresher.renew = { throw NetworkException(NetworkError.Unauthorized()) }
            val client = server { "never" }

            assertFailsWith<NetworkException> { client.get<JsonObject>("/me") }

            assertEquals(expected = SessionState.Expired, actual = session.state.value)
            assertNull(store.tokens)
        }
    }

    @Test
    fun `fresh tokens the server keeps rejecting end the session after the retries`() {
        runTest {
            session.signIn(Tokens(accessToken = "t0", refreshToken = "r"))
            var issued = 0
            refresher.renew = { Tokens(accessToken = "t${++issued}", refreshToken = "r") }
            val client = server { "never" }

            assertFailsWith<NetworkException> { client.get<JsonObject>("/me") }

            assertEquals(expected = 3, actual = refresher.calls)
            assertEquals(expected = SessionState.Expired, actual = session.state.value)
        }
    }

    @Test
    fun `an optional route falls back to guest`() {
        runTest {
            session.signIn(Tokens(accessToken = "old", refreshToken = "r1"))
            refresher.renew = { null }
            val client = server { "never" }

            client.get<JsonObject>("/feed")

            assertEquals(expected = listOf("Bearer old", null), actual = seenTokens)
        }
    }

    @Test
    fun `the session is restored from the store and signs out`() {
        runTest {
            store.tokens = Tokens(accessToken = "saved")
            val restored = SessionManagerImpl(store, refresher)

            assertEquals(expected = "saved", actual = restored.accessToken())
            assertEquals(expected = SessionState.Authenticated, actual = restored.state.value)

            restored.signOut()
            assertEquals(expected = SessionState.Guest, actual = restored.state.value)
            assertNull(store.tokens)
        }
    }
}

private const val CONCURRENT_REQUESTS = 5
private const val REFRESH_DELAY_MILLIS = 50L
