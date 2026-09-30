package io.thernal.networkkit.network.testing

import io.thernal.networkkit.network.api.data.auth.TokenRefresher
import io.thernal.networkkit.network.api.data.auth.TokenStore
import io.thernal.networkkit.network.api.data.auth.Tokens

/** A [TokenStore] in memory; [tokens] is what it holds now. */
class FakeTokenStore(
    var tokens: Tokens? = null,
) : TokenStore {
    override suspend fun read(): Tokens? {
        return tokens
    }

    override suspend fun write(tokens: Tokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}

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
