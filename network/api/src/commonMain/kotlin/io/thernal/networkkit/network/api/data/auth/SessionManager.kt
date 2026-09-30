package io.thernal.networkkit.network.api.data.auth

import kotlinx.coroutines.flow.StateFlow

/** An access token and the refresh token that renews it. */
data class Tokens(
    val accessToken: String,
    val refreshToken: String? = null,
)

/**
 * Where the tokens are kept — the app implements it, over its secure storage (storage-kit's
 * `SecureKeyValueStore`). Only [SessionManager] calls it.
 */
interface TokenStore {
    suspend fun read(): Tokens?

    suspend fun write(tokens: Tokens)

    suspend fun clear()
}

/**
 * Renews [current] at the app's refresh endpoint: the new tokens, or null when the server rejected
 * the refresh token — the session is over. A transient failure (offline, timeout) throws a
 * `NetworkException` instead, and the session is kept.
 */
fun interface TokenRefresher {
    suspend fun refresh(current: Tokens): Tokens?
}

sealed interface SessionState {
    /** Not read from the store yet. */
    data object Unknown : SessionState

    /** No tokens: requests go out without `Authorization`. */
    data object Guest : SessionState

    data object Authenticated : SessionState

    /** The server rejected the session; the tokens are gone. Show sign-in, then [SessionManager.signIn]. */
    data object Expired : SessionState
}

enum class RefreshOutcome {
    /** New tokens are in place — or another request already refreshed past the stale token. */
    Refreshed,

    /** The server rejected the refresh; the session is now [SessionState.Expired]. */
    Rejected,

    /** The refresh could not be tried (offline, timeout); the session is kept. */
    Failed,
}

/**
 * Owns the session: its [state], the tokens through [TokenStore], and renewing them through
 * [TokenRefresher]. The auth interceptor asks it for the token and to refresh; nothing else in the
 * network layer knows about tokens. Screens observe [state] — an [SessionState.Expired] is where the
 * app sends the user to sign in.
 */
interface SessionManager {
    val state: StateFlow<SessionState>

    /** The current access token, or null for a guest. */
    suspend fun accessToken(): String?

    suspend fun signIn(tokens: Tokens)

    /** The user signed out: tokens cleared, state [SessionState.Guest]. */
    suspend fun signOut()

    /**
     * Renews the tokens once for every caller that saw [staleAccessToken] fail: concurrent callers
     * share one refresh, and a caller whose token was already replaced gets [RefreshOutcome.Refreshed]
     * without another one.
     */
    suspend fun refresh(staleAccessToken: String): RefreshOutcome

    /** The server keeps rejecting fresh tokens: tokens cleared, state [SessionState.Expired]. */
    suspend fun expire()
}
