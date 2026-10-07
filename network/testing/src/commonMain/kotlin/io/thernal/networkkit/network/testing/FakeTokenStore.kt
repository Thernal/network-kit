package io.thernal.networkkit.network.testing

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
