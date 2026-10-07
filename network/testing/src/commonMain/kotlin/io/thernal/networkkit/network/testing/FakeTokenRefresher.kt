package io.thernal.networkkit.network.testing

import io.thernal.networkkit.network.api.data.auth.TokenRefresher
import io.thernal.networkkit.network.api.data.auth.Tokens

/**
 * A [TokenRefresher] that answers with [renew] — new tokens, null to reject, or a thrown
 * `NetworkException` for a transient failure — and counts its [calls].
 */
class FakeTokenRefresher(
    var renew: suspend (Tokens) -> Tokens? = { null },
) : TokenRefresher {
    var calls: Int = 0
        private set

    override suspend fun refresh(current: Tokens): Tokens? {
        calls++
        return renew(current)
    }
}
