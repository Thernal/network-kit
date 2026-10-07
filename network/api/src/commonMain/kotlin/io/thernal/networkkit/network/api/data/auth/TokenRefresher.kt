package io.thernal.networkkit.network.api.data.auth

/**
 * Renews [current] at the app's refresh endpoint: the new tokens, or null when there is nothing to
 * refresh with. Call the endpoint through an `ApiClient` and let its `NetworkException` through: one the
 * server answered (401, 403, another rejection) ends the session; offline, a timeout, an unavailable or
 * rate-limiting server keep it.
 */
fun interface TokenRefresher {
    suspend fun refresh(current: Tokens): Tokens?
}
