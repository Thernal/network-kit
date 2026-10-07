package io.thernal.networkkit.network.api.data.auth

import io.thernal.networkkit.network.api.domain.NetworkError
import io.thernal.networkkit.network.api.domain.NetworkException
import io.thernal.networkkit.network.api.domain.NetworkResult
import io.thernal.networkkit.network.api.domain.map
import io.thernal.networkkit.network.api.domain.networkCall
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiRoutePatternTest {
    @Test
    fun `matches by method and segments`() {
        val pattern = ApiRoutePattern(method = "POST", path = "/users/{id}/avatar")

        assertTrue(pattern.matches(method = "post", path = "/users/42/avatar"))
        assertTrue(pattern.matches(method = "POST", path = "users/42/avatar/?size=2"))
        assertFalse(pattern.matches(method = "GET", path = "/users/42/avatar"))
        assertFalse(pattern.matches(method = "POST", path = "/users/42"))
    }

    @Test
    fun `a wildcard and a null method match anything`() {
        val pattern = ApiRoutePattern(method = null, path = "/public/*")

        assertTrue(pattern.matches(method = "DELETE", path = "/public/x"))
        assertFalse(pattern.matches(method = "GET", path = "/public/x/y"))
    }

    @Test
    fun `network call turns network exceptions into failures`() {
        runTest {
            val failure = networkCall<Int> { throw NetworkException(NetworkError.Timeout) }
            val success = networkCall { 2 }.map { it * 2 }

            assertEquals(expected = NetworkResult.Failure(NetworkError.Timeout), actual = failure)
            assertEquals(expected = NetworkResult.Success(4), actual = success)
        }
    }
}
