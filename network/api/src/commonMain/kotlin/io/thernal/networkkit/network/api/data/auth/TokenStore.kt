package io.thernal.networkkit.network.api.data.auth

/**
 * Where the tokens are kept — the app implements it, over its secure storage (storage-kit's
 * `SecureKeyValueStore`). Only [SessionManager] calls it.
 */
interface TokenStore {
    suspend fun read(): Tokens?

    suspend fun write(tokens: Tokens)

    suspend fun clear()
}
